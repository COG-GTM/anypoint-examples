# HTTP Request-Response with Logger — Spring Boot

Spring Boot 3 migration of [`http-request-response-with-logger`](../../http-request-response-with-logger). Any request to any path returns the request path as the body and logs `About to echo <path>` at INFO.

## Run

```bash
cd spring-boot-migration
mvn -B -pl http-request-response-with-logger -am verify          # build + parity tests
mvn -pl http-request-response-with-logger spring-boot:run        # or: java -jar http-request-response-with-logger/target/http-request-response-with-logger-1.0.0-SNAPSHOT.jar
```

The port defaults to `8081` and honours the Mule property: `-Dhttp.port=9090`.

```bash
curl http://localhost:8081/echo    # -> /echo
```

## Parity tests

`EchoParityTest` replays the 6 fixtures in `src/test/resources/parity/http-request-response-with-logger/` (MUnit `GET /message`, README `/echo` and `/moon`, nested path, query string not echoed, POST). `EchoLoggerSideEffectTest` checks the logger side effect.

Run against a live Mule runtime as well: `mvn -pl http-request-response-with-logger -am verify -Dparity.mule.base-url=http://localhost:8081`.

## Behavioural deltas

- **`Content-Type` of the echo.** The Mule flow sets a String payload with no MIME type and Mule doesn't document the `Content-Type` it sends. Spring sends `text/plain;charset=UTF-8`. The fixtures don't assert this header.
- **No springdoc UI.** The catch-all `/**` route would hide springdoc's `/v3/api-docs` and `/swagger-ui` paths, and Mule echoes those paths too, so springdoc is left out of this module on purpose.

Mule ↔ Spring mapping: [MIGRATION-NOTES.md](MIGRATION-NOTES.md).
