# http-oauth-provider (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`http-oauth-provider`](../../http-oauth-provider).
Mule's `oauth2-provider:config` (authorization server) runs on **Spring Authorization Server**, and `oauth2-provider:validate`
runs on the **Spring Security resource server** (opaque bearer tokens checked against the authorization server's own token store,
with no remote introspection call, like Mule's in-memory token store).

| Port | Endpoint | Behaviour (same as Mule) |
|---|---|---|
| 8081 (`http.provider.port`) | `GET /authorize?response_type=code&client_id=myclientid&scope=READ_RESOURCE&redirect_uri=http://localhost:8082/redirect` | `SampleAPI` login form (`id="username"`, `id="password"`, **Login and Authorize**) |
| 8081 | form post of `mule`/`mule` to the same URL | `302` to `redirect_uri?code=<authorization code>` |
| 8081 | `GET /token?grant_type=authorization_code&client_id=..&client_secret=..&code=..&redirect_uri=..` (also `POST` form) | `{"scope":"READ_RESOURCE","expires_in":86400,"token_type":"bearer","access_token":"<opaque>"}` |
| 8082 (`http.listener.port`) | `GET /resources` + `Authorization: Bearer <token>` | `{"name":"payroll","uri":"http://localhost:8082/resources/payroll"}`, or `401` without a valid `READ_RESOURCE` token |
| 8082 | `GET /redirect?code=..` | `302` to `http://localhost:8081/token?grant_type=authorization_code&&client_id=myclientid&client_secret=myclientsecret&code=..&redirect_uri=http://localhost:8082/redirect` |

Client `myclientid` / `myclientsecret` ("Mule Bookstore", confidential, `AUTHORIZATION_CODE` only, scopes `READ_RESOURCE POST_RESOURCE`,
redirect URI pattern `http://localhost*`). Resource owner `mule` / `mule` (authority `RESOURCE_OWNER`). Tokens live 86400 s. No refresh tokens.

## Run

```bash
cd spring-boot-migration
mvn -pl http-oauth-provider -am spring-boot:run
# browser: http://localhost:8081/authorize?response_type=code&client_id=myclientid&scope=READ_RESOURCE&redirect_uri=http://localhost:8082/redirect
# log in as mule/mule. The browser follows /redirect to /token and shows the token JSON.
curl -H "Authorization: Bearer <access_token>" http://localhost:8082/resources
```

## Tests

`mvn -pl http-oauth-provider -am verify` runs:

- `ProviderParityTest`: 5 JSON fixtures in `src/test/resources/parity/http-oauth-provider/` (login page, login → code redirect,
  `redirectFlow` Location, `/resources` without a token or with an unknown one).
- `ProviderGrantFlowTest`: the README's OAuth dance end to end. Codes and tokens only exist at run time, so the fixtures in
  `src/test/resources/parity/http-oauth-provider-flow/` are templates (`{{code}}`, `{{access_token}}`) filled in by the test and compared with
  `ParityComparator`: GET token exchange, POST token exchange, protected resource. The test also checks that codes are single-use,
  that a wrong password does not redirect, and that a `redirect_uri` outside `http://localhost*` is rejected.
- `ProviderConnectorTest`: `/authorize` and `/token` are served only on the provider port, `/resources` and `/redirect` only on the listener port.

The fixtures use one random server port for every listener, so the `parity` profile turns the provider connector off
(`provider.connector-enabled=false`).

## Behavioural deltas

1. **Login page markup.** The page has the same title, field ids and button text the Mule README and `HttpOauthProviderIT` rely on, but
   its HTML is not Mule's.
2. **Error responses are not documented for Mule, so Spring Authorization Server's are used.** Wrong credentials re-show the login
   page with `401`. An invalid `redirect_uri` gives `400`. Token errors use the RFC 6749 shape (`400 {"error":"invalid_grant"}`). A
   rejected `/resources` call gives `401` with `WWW-Authenticate: Bearer ...`. The fixtures assert only the documented `401` status.
3. **Token value format.** Access tokens are opaque URL-safe random strings from Spring Authorization Server, not Mule's token format.
   Clients only use them as an opaque value.
4. **Extra endpoints.** Spring Authorization Server also exposes its standard token revocation, introspection and authorization-server
   metadata endpoints on the provider. Mule's provider exposed none of them in this configuration.
5. **Token endpoint client authentication.** Besides `client_id`/`client_secret` parameters (Mule's way), HTTP Basic client
   authentication is accepted too.
