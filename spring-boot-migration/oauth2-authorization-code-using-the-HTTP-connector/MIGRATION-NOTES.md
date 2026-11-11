# Migration notes: `oauth2-authorization-code-using-the-HTTP-connector`

- Original Mule app: [`../../oauth2-authorization-code-using-the-HTTP-connector`](../../oauth2-authorization-code-using-the-HTTP-connector)
- Mule flows: `http-authorization-code-web.xml` → `boxUserLoginFlow`, `userLoginDoneFlow` (plus the two listeners the grant type opens)
- Spring Boot module: `spring-boot-migration/oauth2-authorization-code-using-the-HTTP-connector`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener-config HTTP_Listener_Configuration` (localhost:8081) | embedded Tomcat, `server.port=${http.port:8081}` | |
| `http:listener path="web"` + `http:response-builder statusCode=302` + `Location` header (`boxUserLoginFlow`) | `WebController#web` | |
| `logger` "Redirecting user to Box login page" | `WebController#web` SLF4J `log.info` | |
| `oauth2:authorization-code-grant-type clientId/clientSecret` | `OAuthClientConfig#boxRegistration` (`AuthorizationGrantType.AUTHORIZATION_CODE`, `CLIENT_SECRET_POST`) | Secret sent in the form body, as Mule does. |
| `oauth2:authorization-request authorizationUrl` + `custom-parameters` | `BoxAuthorizationCodeClient#authorizationRequestLocation` (`OAuth2AuthorizationRequest` + `additionalParameters`) | Parameter order matches Mule's `AuthorizationRequestUrlBuilder`. |
| `localAuthorizationUrl` listener (Mule `AuthorizationRequestHandler`) | `OAuthCallbackEndpoints` router function on the path of `oauth.local-authorization-url` | |
| `redirectionUrl` listener (Mule `AutoAuthorizationCodeTokenRequestHandler`) | `OAuthCallbackEndpoints` → `BoxAuthorizationCodeClient#handleRedirect` | Same status codes, texts and `authorizationStatus` values. |
| `StateEncoder` / `StateDecoder` (`:resourceOwnerId=`, `:onCompleteRedirectTo=`) | `MuleOAuthState` | |
| `oauth2:token-request tokenUrl` | `DefaultAuthorizationCodeTokenResponseClient` (`box.token-url`) | `TokenUrlStatusException` keeps the HTTP status for the 200 status code. |
| `oauth2:token-response` + `custom-parameter-extractor token_type` | `OAuth2AccessTokenResponseHttpMessageConverter` with `token_type` defaulting to `bearer` | |
| access + refresh token both required (`TokenNotFoundException`) | `BoxAuthorizationCodeClient#handleRedirect` | 201 when either is missing |
| `oauth2:token-manager-config` (in-memory, resource owner `default`) | `InMemoryOAuth2AuthorizedClientService`, principal `default` | |
| Default `refreshTokenWhen` (401/403) + refresh-token grant (adds `redirect_uri`) | `WebController#loginDone` → `BoxAuthorizationCodeClient#refreshAccessToken` (`DefaultRefreshTokenTokenResponseClient` with a `redirect_uri` parameter converter) | |
| Callback listeners on their own host:port (8082, `TLS_Context`) | `CallbackConnectorConfig` (second Tomcat connector, optional TLS, port separation filter) | See deltas. |
| `http:request-config HTTP_Request_Configuration_HTTPS` (https://api.box.com:443, basePath `/2.0`) | `RestClient` + `box.api-base-url` | |
| `http:request GET /search` + `query-param query=mule` (`userLoginDoneFlow`) | `WebController#search` | |
| `object-to-string-transformer` + `set-payload` | `WebController#loginDone` string concatenation | |
| `SuccessStatusCodeValidator` (0..399) / listener 500 on exceptions | `MuleFlowException` + `FlowExceptionHandler` (500, message as body) | |
| `tls:context TLS_Context` | `oauth.callback.ssl.*` (`keystore.*` properties) for the callback connector; JVM trust store outbound | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `box.id` | `box.id` | (none) |
| `box.secret` | `box.secret` | (none) |
| `keystore.password`, `keystore.keyPassword` (+ `keystore.jks`) | `oauth.callback.ssl.key-store-password`, `key-password`, `key-store` (`${keystore.path}`) | TLS off when no key store |
| listener port `8081` | `server.port` (`${http.port:8081}`) | 8081 |
| `redirectionUrl` | `oauth.redirection-url` | `https://localhost:8082/redirectUrl` |
| `localAuthorizationUrl` | `oauth.local-authorization-url` | `https://localhost:8082/authorization` |
| `authorizationUrl` | `box.authorization-url` | `https://www.box.com/api/oauth2/authorize` |
| `tokenUrl` | `box.token-url` | `https://www.box.com/api/oauth2/token` |
| request config `api.box.com:443` + `/2.0` | `box.api-base-url` | `https://api.box.com/2.0` |
| `/web` `Location` header | `web.login-redirect-location` | as in the flow XML |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-web-redirects-to-local-authorization-url` | `/web` → 302 with the exact `Location` | documented |
| `02-login-done-without-token-is-500` | no token yet → 500 `No access token for the default user...` | documented |
| `03-local-authorization-redirects-to-box` | `/authorization` → 302 to Box with exact parameters, order, encoding and state | documented |
| `04-local-authorization-without-on-complete` | no state parameter when there is nothing to encode | documented |
| `05-redirect-url-without-code-is-400` | missing `code` → 400 with Mule's text, no token call | documented |
| `06-redirect-url-token-url-error-is-500` | token URL 401 → 500 `Failure calling token url ...` | documented |
| `07-redirect-url-missing-refresh-token-redirects-with-201` | no `refresh_token` → 302 `authorizationStatus=201` | documented |
| `08-redirect-url-exchanges-code-and-redirects` | exact token form, no `Authorization` header, 302 `authorizationStatus=0` | documented |
| `09-login-done-searches-box-with-token` | `/web/loginDone` sends the stored Bearer token, final text | documented |
| `10-login-done-box-error-is-500` | Box 500 → 500 `Response code 500 mapped as failure.` | documented |
| `AuthorizationCodeGrantFlowTest` (2 tests) | full browser journey, refresh-token grant after 401 | documented (Mule README, `HttpAuthorizationCodeWebIT`, Mule 3.8 OAuth module source) |
| `CallbackConnectorTest` | listeners stay on their Mule ports | documented (flow XML) |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas): callback TLS only with a configured key store, JVM trust store outbound, and
`token_type` defaulting to `bearer`.
