# Migration notes: `munit-short-tutorial`

- Original Mule app: [`../../munit-short-tutorial`](../../munit-short-tutorial)
- Mule flows: `production-code.xml` → `exampleFlow`, `exampleFlow2`, `exampleSub_Flow1`, `exampleSub_Flow2`
- Spring Boot module: `spring-boot-migration/munit-short-tutorial`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener allowedMethods="GET" path="/"` (`exampleFlow`) | `ExampleFlow#receive` (`@GetMapping("/")`) |  |
| `set-payload #[message.inboundProperties['http.query.params']['url_key']]` | `@RequestParam(name="url_key", required=false)` → `FlowEvent` payload |  |
| `flow-ref name="exampleFlow2"` + `choice` on `flowVars['my_variable']` | `ExampleFlow#process` calling `ExampleFlow2#process` |  |
| `exampleFlow2` `choice` on `'payload_1'.equals(payload)` | `ExampleFlow2#process` |  |
| `sub-flow exampleSub_Flow1/2` (`set-variable my_variable`) | `SubFlows#exampleSubFlow1/2` | Flow variables are a map on `FlowEvent` |
| MUnit `mock:verify-call messageProcessor="mule:sub-flow"` | Mockito `spy(SubFlows)` + `verify(..., times(1))` in `ProductionCodeTestSuiteTest` | `doc-test-exampleFlow2Test1/2` |
| MUnit `mock:when messageProcessor="mule:flow"` returning `my_variable` | Mockito `mock(ExampleFlow2)` answering a `FlowEvent` with `my_variable` | `doc-test-exampleFlow-unit-Test_1/2` |
| MUnit functional tests (`http:request` to the listener) | Parity fixtures `01`/`02` (`MunitShortTutorialParityTest`) | `doc-test-exampleFlow-functionalTest_1/2` |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port}`) | 9090 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-url-key-payload-1.json` | url_key=payload_1 routes to exampleSub_Flow1 (my_variable=var_value_1) so the response is response_payload_1. | `munit` |
| `02-url-key-payload-2.json` | url_key=payload_2 routes to exampleSub_Flow2 (my_variable=var_value_2) so the response is response_payload_2. | `munit` |
| `03-url-key-missing.json` | Without url_key the payload is null, 'payload_1'.equals(payload) is false, so exampleFlow2 takes the otherwise branch and the response is response_payload_2. | `documented` |
| `04-post-not-allowed.json` | The listener declares allowedMethods=GET, so other methods are rejected with 405 Method Not Allowed. | `documented` |

## Behavioural deltas

- Rejected methods (e.g. `POST /`) return 405 in both, but the body differs: Spring Boot's JSON error document vs Mule's plain-text listener message. The fixture asserts only the status.
