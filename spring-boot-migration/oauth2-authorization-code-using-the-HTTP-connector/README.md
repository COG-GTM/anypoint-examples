# oauth2-authorization-code-using-the-HTTP-connector (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example
[`oauth2-authorization-code-using-the-HTTP-connector`](../../oauth2-authorization-code-using-the-HTTP-connector).
Mule's `oauth2:authorization-code-grant-type` on the Box request config is replaced by **Spring Security OAuth2 Client**
(`ClientRegistration` with `authorization_code`, `DefaultAuthorizationCodeTokenResponseClient`,
`DefaultRefreshTokenTokenResponseClient`, `OAuth2AuthorizedClientService`). The Mule-specific parts of the dance (the local
authorization URL, the `:onCompleteRedirectTo=` state suffix, the `authorizationStatus` codes and the callback texts) are kept in
`BoxAuthorizationCodeClient` and `MuleOAuthState`.

| Port | Endpoint | Behaviour (same as Mule) |
|---|---|---|
| 8081 | `/web` | `302 Location: https://localhost:8082/authorization?onCompleteRedirectTo=http://localhost:8081/web/loginDone` |
| 8082 | `/authorization` | `302` to `https://www.box.com/api/oauth2/authorize?response_type=code&client_id=..&state=..&box_device_id=1&box_device_name=device1&redirect_uri=..` |
| 8082 | `/redirectUrl` | Exchanges `code` at the token URL (form POST: `code`, `client_id`, `client_secret`, `grant_type=authorization_code`, `redirect_uri`), stores access + refresh token for user `default`, answers `Successfully retrieved access token` or `302` to `onCompleteRedirectTo?authorizationStatus=<0/100/200/201/300>` |
| 8081 | `/web/loginDone` | `GET https://api.box.com/2.0/search?query=mule` with the stored Bearer token (refresh + resend once on 401/403), answers `The user successfully authorized access to the resource. Searching for files containing "mule": <Box body>` |

## Run

```bash
cd spring-boot-migration
mvn -pl oauth2-authorization-code-using-the-HTTP-connector -am spring-boot:run -Dspring-boot.run.arguments="\
  --box.id=<client id> --box.secret=<client secret> \
  --keystore.path=file:/path/keystore.jks --keystore.password=... --keystore.keyPassword=..."
# then browse to http://localhost:8081/web
```

Without `keystore.path` the 8082 callback listener runs as plain HTTP (see deltas). The Box app must have
`https://localhost:8082/redirectUrl` as its redirect URI, as in the Mule README.

## Tests

`mvn -pl oauth2-authorization-code-using-the-HTTP-connector -am verify` runs:

- `AuthorizationCodeParityTest`: 10 JSON fixtures in `src/test/resources/parity/oauth2-authorization-code-using-the-HTTP-connector/`, run in
  file order as one grant flow against the Spring app with WireMock as the Box token endpoint and Box API: redirects, error cases
  before a token exists, the code exchange (exact form fields, no `Authorization` header), the completion redirect and the
  Box search with the stored token.
- `AuthorizationCodeGrantFlowTest`: the browser journey hop by hop (`/web` → `/authorization` → Box consent simulated → `/redirectUrl`
  → `/web/loginDone`) and the refresh-token grant after a 401.
- `CallbackConnectorTest`: `/web*` is served only on the main port and the OAuth callbacks only on the callback port.

Box is a SaaS API, so it is stubbed with WireMock (`box.token-url` / `box.api-base-url` point at it in the `parity` profile). The
fixtures use one random server port for every listener, so the `parity` profile turns the second connector off
(`oauth.callback.connector-enabled=false`).

## Behavioural deltas

1. **Callback listener TLS.** Mule's callback listener on 8082 always uses `TLS_Context` (`keystore.jks`). Spring enables TLS on the
   extra connector only when `keystore.path` is set. Without it the callback is served as plain HTTP on the same port and path.
   The authorization URL still sends the configured `redirect_uri` (`https://localhost:8082/redirectUrl`).
2. **Outbound trust store.** The `TLS_Context` trust store is not carried over. Box is called with the JVM default trust store.
3. **Token responses without `token_type`.** Spring Security requires a `token_type`. When Box omits it, `bearer` is assumed. Mule did
   not need it. No change visible to callers.
