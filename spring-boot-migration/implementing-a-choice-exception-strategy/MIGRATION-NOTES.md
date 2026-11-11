# Migration notes: `implementing-a-choice-exception-strategy`

- Original Mule app: [`../../implementing-a-choice-exception-strategy`](../../implementing-a-choice-exception-strategy)
- Mule flows: `choice-error-handling.xml` → `choice-error-handlingFlow1`
- Spring Boot module: `spring-boot-migration/implementing-a-choice-exception-strategy`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/"` + `http:response-builder statusCode/reasonPhrase` from flow vars | `ChoiceErrorHandlingController#handle` (`@PostMapping("/")`) returning `ResponseEntity` |  |
| `json:json-to-object-transformer returnClass="java.util.HashMap"` | Jackson `ObjectMapper#readValue(body, LinkedHashMap.class)` | Insertion order is kept so that the documented `HashMap` rendering can be reproduced (see `MulePayloadRenderer`) |
| `custom-filter class="org.mule.examples.InputDataValidator"` | `InputDataValidator#validate` | Same checks, exception types and messages |
| `regex-filter pattern=…` on `#[payload['email']]` | `EmailFilter#accept` (`Pattern#find`, as Mule's `RegExFilter` does) | A rejected message stops the flow and returns an empty 200 response |
| `choice-exception-strategy` / `catch-exception-strategy when="#[exception.causedBy(...)]"` | `ChoiceExceptionStrategyAdvice` (`@ControllerAdvice`) + `FlowException#causedBy` | The first matching branch wins, as in Mule |
| `dw:transform-message` setting `statusCode`="400" / `reason` | `ResponseEntity.status(BAD_REQUEST)` | See deltas for the reason phrase |
| `set-payload value="Invalid/Missing input data: #[payload]"` | `"Invalid input data: " + MulePayloadRenderer#render(order)` |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-valid-order.json` | Valid order passes the validator and the email regex filter. | `documented` |
| `02-missing-email.json` | Missing email: InputDataValidator throws NullPointerException; second catch-exception-strategy answers 400 'Missing input data: #[payload]' with the HashMap payload. | `documented` |
| `03-negative-price.json` | Negative item price per unit: IllegalArgumentException; first catch-exception-strategy answers 400 'Invalid input data: #[payload]'. | `documented` |
| `04-zero-units.json` | item units < 1 throws IllegalArgumentException (README: 'This applies to negative item units as well'). | `documented` |
| `05-missing-units.json` | Missing item units: payloadMap.get("item units").toString() throws NullPointerException, so the Missing input data branch answers. | `documented` |
| `06-non-numeric-units.json` | Non-numeric item units: Integer.parseInt throws NumberFormatException, a subclass of IllegalArgumentException, so the Invalid input data branch answers. | `documented` |
| `07-invalid-email-filtered.json` | An email that the regex-filter rejects stops the flow without an exception: the listener answers 200 with an empty body. | `documented` |

## Behavioural deltas

- The custom HTTP reason phrases (`Invalid input data` / `Missing input data`) are not sent. Tomcat (Spring Boot's server) always writes the standard reason phrase, and HTTP/2 has none. Status code and body are unchanged.
- `#[payload]` printed a `java.util.HashMap`. The README's documented output follows the JDK 7 `HashMap` iteration order, so `MulePayloadRenderer` reproduces that order exactly. The same Mule app running on JDK 8+ would print the keys in a different order.
- Only `POST` is mapped. The Mule listener accepted any method, but without a JSON body the transformer failed, so other methods were never useful.
