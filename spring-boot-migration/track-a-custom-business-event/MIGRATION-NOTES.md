# Migration notes: `track-a-custom-business-event`

- Original Mule app: [`../../track-a-custom-business-event`](../../track-a-custom-business-event)
- Mule flows: `custom-business-events.xml` → `custom-business-eventsFlow1`
- Spring Boot module: `spring-boot-migration/track-a-custom-business-event`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="customBusinessEvents"` | `CustomBusinessEventsController#track` (`@RequestMapping("/customBusinessEvents")`) |  |
| `json:json-to-object-transformer returnClass="java.util.HashMap"` | Jackson `ObjectMapper#readValue(body, HashMap.class)` |  |
| `tracking:custom-event event-name="Price"` + `tracking:meta-data` (Groovy) | `ApplicationEventPublisher#publishEvent(new BusinessEvent("Price", …))` | Meta-data values are stored as strings, as Insight did |
| `tracking:transaction id="custom-business-event-example-transaction"` | `BusinessEvent#transactionId` |  |
| `tracking:enable-default-events` / CloudHub Insight | `BusinessEventRecorder` (`@EventListener`): log line, in-memory list and Micrometer counter `business.events{event=Price}` (exposed at `/actuator/metrics`) |  |
| `scripting:transformer` (JRuby `case` discount) | `CustomBusinessEventsController#discountFor` + `price * (1 - discount)` on `double` | Same IEEE-754 arithmetic as JRuby `Float` |
| `object-to-string-transformer` | `Double.toString` (`text/plain`) | `8.5`, `8.0`, … |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-shoes-discount.json` | Shoes get a 15% discount: 10 * (1 - 0.15) = 8.5. A Price business event records the undiscounted order data. | `documented` |
| `02-jeans-discount.json` | Jeans get a 20% discount: 10 * (1 - 0.2) = 8.0 (a JRuby Float, so object-to-string prints 8.0). | `documented` |
| `03-jackets-discount.json` | Jackets get a 30% discount: 10 * (1 - 0.3) = 7.0. | `documented` |
| `04-decimal-price.json` | A decimal unit price is discounted the same way: 20.5 * (1 - 0.2) = 16.400000000000002 (IEEE-754 double, as JRuby/Java print it). | `documented` |
| `05-malformed-json.json` | A body that is not JSON fails json-to-object-transformer before the custom event: Mule's default exception strategy answers 500 and no business event is recorded. | `documented` |

## Behavioural deltas

- CloudHub Insight is replaced by Spring application events, a log line and a Micrometer counter. Events are not shipped to Anypoint.
- Unknown `item name`: the JRuby script left the global `$discount` unchanged. It then either reused the previous request's discount or failed with `nil` on the first request. Spring answers 500 on every request, so the result no longer depends on earlier requests.
