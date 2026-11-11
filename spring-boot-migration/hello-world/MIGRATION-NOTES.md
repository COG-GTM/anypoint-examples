# Migration notes: `hello-world`

- Original Mule app: [`../../hello-world`](../../hello-world)
- Mule flows: `HelloWorld.xml` → `HelloWorldFlow1`
- Spring Boot module: `spring-boot-migration/hello-world`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `HelloWorld.xml:http:listener path="helloWorld"` (`HelloWorldFlow1`) | `HelloWorldController#helloWorld` (`@RequestMapping("/helloWorld")`) | No `allowedMethods` in Mule, so no method restriction in Spring either |
| `set-payload value="Hello World"` | Return value `"Hello World"` (`text/plain`) |  |
| `HelloWorldIT` (FunctionalMunitSuite-style IT) | `HelloWorldParityTest` (`AbstractParityTest`) |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-get-hello-world.json` | GET /helloWorld returns the constant payload 'Hello World' with status 200. | `documented` |
| `02-post-hello-world.json` | The listener has no allowedMethods, so a POST with a body gets the same constant payload; the request body is ignored. | `documented` |

## Behavioural deltas

None.
