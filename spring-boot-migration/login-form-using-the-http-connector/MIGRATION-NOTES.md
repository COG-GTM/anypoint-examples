# Migration notes: `login-form-using-the-http-connector`

- Original Mule app: [`../../login-form-using-the-http-connector`](../../login-form-using-the-http-connector)
- Mule flows: `login-form-using-the-http-connector.xml` → `loginPage`, `doLogin`, `requesterLogin`
- Spring Boot module: `spring-boot-migration/login-form-using-the-http-connector`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener allowedMethods="GET" path="/login"` + `parse-template login/index.html` | `LoginController#loginPage` + `LoginTemplates#page` |  |
| `http:listener allowedMethods="POST" path="/login"` (form body → Map) | `LoginController#doLogin` (`@PostMapping`, `@RequestParam username/password`) |  |
| `logger level=INFO "User #[payload.username] is trying to login"` | `log.info("User {} is trying to login", username)` |  |
| `message-filter` + `expression-filter` (`mule`/`mule`) | `if (VALID_USERNAME.equals(...) && VALID_PASSWORD.equals(...))` |  |
| `parse-template loginSuccessful.html` + `set-property Content-Type=text/html; charset=UTF-8` | `LoginTemplates#success` + `ResponseEntity.contentType(text/html;charset=UTF-8)` | `#[payload.username]` replaced, HTML-escaped |
| `catch-exception-strategy` → `set-property http.status=403` + `parse-template loginFailure.html` | `ResponseEntity.status(403)` + `LoginTemplates#failure` |  |
| `http:listener path="/requesterLogin"` + `expression-component` (HashMap form) + `http:request POST /login` | `LoginController#requesterLogin` with `RestClient` POSTing `application/x-www-form-urlencoded` | Real HTTP loopback, like Mule |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `http.port` (requester config) | `login.request.base-url` | blank = `http://localhost:<local.server.port>` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| 01-get-login-page | Form HTML verbatim | munit |
| 02-login-valid | 200, `text/html; charset=UTF-8`, success page | munit |
| 03-login-wrong-password | 403 + failure page | documented |
| 04-login-unknown-user | 403 + failure page with that username | documented |
| 05-requester-login | Loopback login → success page | munit |
| 06-put-login-not-allowed | PUT → 405 | documented |
| `LoginLoggerSideEffectTest` | INFO log line | documented |

## Behavioural deltas

- **The username is HTML-escaped in the templates.** Mule's `parse-template` inserts `#[payload.username]` raw, which allows reflected XSS. Spring escapes it with `HtmlUtils.htmlEscape`. Output differs only for usernames that contain `<`, `>`, `&`, `"` or `'`.
- **`Content-Type` of the 403 and of `GET /login`.** Mule doesn't set one on those responses. Spring sends `text/html`. The fixtures don't assert it. The 200 login keeps Mule's explicit `text/html; charset=UTF-8`.
- **`/requesterLogin` loopback target.** Mule's `http:request-config` calls `0.0.0.0:${http.port}`. Spring calls `http://localhost:<actual server port>`, or `login.request.base-url` if that is set. This is the same app, just reached by a routable host name.
- **Added springdoc endpoints.** `/v3/api-docs` and `/swagger-ui.html` are new paths.
