# Migration notes: `http-request-response-with-logger`

- Original Mule app: [`../../http-request-response-with-logger`](../../http-request-response-with-logger)
- Mule flows: `echo.xml` → `EchoFlow`
- Spring Boot module: `spring-boot-migration/http-request-response-with-logger`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `echo.xml:http:listener-config port=${http.port}` | `server.port: ${http.port:8081}` (`application.yml`) |  |
| `echo.xml:http:listener path="/*"` (`EchoFlow`) | `EchoController#echo` (`@RequestMapping("/**")`) | Every method, like the Mule listener (no `allowedMethods`) |
| `set-payload value="#[message.inboundProperties['http.request.path']]"` | `HttpServletRequest#getRequestURI()` minus the context path | Query string excluded, like `http.request.path` |
| `logger level="INFO" message="About to echo #[message.payload]"` | SLF4J `log.info("About to echo {}", path)` |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| 01-echo-message | GET /message → `/message` | munit |
| 02-echo-echo | GET /echo → `/echo` | documented |
| 03-echo-moon | GET /moon → `/moon` | documented |
| 04-echo-nested-path | Nested paths are echoed in full | documented |
| 05-echo-ignores-query | The query string isn't part of the echo | documented |
| 06-echo-post | POST is accepted and echoed | documented |
| `EchoLoggerSideEffectTest` | INFO log line `About to echo /moon` | documented |

## Behavioural deltas

- **`Content-Type` of the echo.** The Mule flow sets a String payload with no MIME type and Mule doesn't document the `Content-Type` it sends. Spring sends `text/plain;charset=UTF-8`. The fixtures don't assert this header.
- **No springdoc UI.** The catch-all `/**` route would hide springdoc's `/v3/api-docs` and `/swagger-ui` paths, and Mule echoes those paths too, so springdoc is left out of this module on purpose.
