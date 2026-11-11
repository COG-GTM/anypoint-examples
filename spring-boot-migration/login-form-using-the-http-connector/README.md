# Login Form using the HTTP Connector — Spring Boot

Spring Boot 3 migration of [`login-form-using-the-http-connector`](../../login-form-using-the-http-connector). `GET /login` serves an HTML form. `POST /login` accepts only `mule`/`mule` (200 + success page) and answers 403 + a failure page otherwise. `/requesterLogin` logs in through an HTTP call to the app's own `/login`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl login-form-using-the-http-connector -am verify          # build + parity tests
mvn -pl login-form-using-the-http-connector spring-boot:run        # or: java -jar login-form-using-the-http-connector/target/login-form-using-the-http-connector-1.0.0-SNAPSHOT.jar
```

The port defaults to `8081` and honours the Mule property: `-Dhttp.port=9090`.

Open `http://localhost:8081/login` in a browser, or:

```bash
curl -d 'username=mule&password=mule' http://localhost:8081/login
curl http://localhost:8081/requesterLogin
```

## Parity tests

`LoginParityTest` replays the 6 fixtures in `src/test/resources/parity/login-form-using-the-http-connector/` (form page, valid login with `text/html; charset=UTF-8`, wrong password → 403, unknown user → 403, requester loopback, PUT → 405). `LoginLoggerSideEffectTest` checks the INFO log line `User mule is trying to login`.

Run against a live Mule runtime as well: `mvn -pl login-form-using-the-http-connector -am verify -Dparity.mule.base-url=http://localhost:8081`.

## Behavioural deltas

- **The username is HTML-escaped in the templates.** Mule's `parse-template` inserts `#[payload.username]` raw, which allows reflected XSS. Spring escapes it with `HtmlUtils.htmlEscape`. Output differs only for usernames that contain `<`, `>`, `&`, `"` or `'`.
- **`Content-Type` of the 403 and of `GET /login`.** Mule doesn't set one on those responses. Spring sends `text/html`. The fixtures don't assert it. The 200 login keeps Mule's explicit `text/html; charset=UTF-8`.
- **`/requesterLogin` loopback target.** Mule's `http:request-config` calls `0.0.0.0:${http.port}`. Spring calls `http://localhost:<actual server port>`, or `login.request.base-url` if that is set. This is the same app, just reached by a routable host name.
- **Added springdoc endpoints.** `/v3/api-docs` and `/swagger-ui.html` are new paths.

Mule ↔ Spring mapping: [MIGRATION-NOTES.md](MIGRATION-NOTES.md).
