# Migration notes: `oauth2-client-credentials-using-the-HTTP-connector`

- Original Mule app: [`../../oauth2-client-credentials-using-the-HTTP-connector`](../../oauth2-client-credentials-using-the-HTTP-connector)
- Mule flows: `http-client-credentials.xml` → `httpclientcredentialsFlow`, `http-client-credentialsFlow`
- Spring Boot module: `spring-boot-migration/oauth2-client-credentials-using-the-HTTP-connector`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener-config HTTP_Listener_Configuration` (0.0.0.0:8081) | embedded Tomcat, `server.port=${http.port:8081}` | |
| `oauth2:client-credentials-grant-type clientId/clientSecret` | `OAuthClientConfig#boxRegistration` (`ClientRegistration`, `AuthorizationGrantType.CLIENT_CREDENTIALS`, `CLIENT_SECRET_BASIC`) | Basic header is built from the raw id/secret, as Mule does (no extra URL-encoding). |
| `oauth2:token-request tokenUrl` | `ClientRegistration.tokenUri` (`box.token-url`) | |
| `oauth2:token-response` + `custom-parameter-extractor token_type` | Spring `OAuth2AccessTokenResponse` (token type kept) | |
| `oauth2:token-manager-config` (in-memory token store, resource owner `default`) | `InMemoryOAuth2AuthorizedClientService` behind `BoxTokenManager` (principal `default`) | |
| client-credentials token fetch on `start()` | `BoxTokenManager#fetchTokenOnStartup` (`@EventListener(ApplicationStartedEvent)`) | |
| Default `refreshTokenWhen` (401/403) + resend | `ClientCredentialsController#callBox` (refresh with `BoxTokenManager#refreshAccessToken`, send once more) | |
| `http:request-config HTTP_Request_Configuration` (https://app.box.com:443, basePath `/api/oauth2/authorize`) | `OutboundHttp` + `box.base-url`/`box.base-path` | `path="/"` → `/api/oauth2/authorize/` |
| `tls:context TLS_Context` | JVM default trust store | See Behavioural deltas. |
| `logger message="#[payload]"` | `ClientCredentialsController` SLF4J `log.info` | |
| `rollback-exception-strategy` + logger | `FlowExceptionHandler` (`@RestControllerAdvice`, logs and answers 500 with the message) | No transaction to roll back; HTTP listener answers 500 as Mule does. |
| `http:listener path="/FIM/*"` | `ClientCredentialsController#fim` (`@RequestMapping("/FIM/**")`) | |
| `http:request HTTP_Request_Configuration1 path=#[http.request.uri] method=#[http.method]` | `OutboundHttp#exchange` with inbound method, path, query, body, `Content-Type` | |
| `http:uri-param client_id` | not sent | The path has no `{client_id}` placeholder, so Mule never sends it either. |
| `SuccessStatusCodeValidator` (0..399) | `OutboundHttp` raises `MuleFlowException("Response code N mapped as failure.")` for status >= 400 | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `oauth.client.id` | `oauth.client.id` | (none) |
| `oauth.client.secret` | `oauth.client.secret` | (none) |
| `client.id` | `client.id` | (none, unused like in Mule) |
| listener port `8081` | `server.port` (`${http.port:8081}`) | 8081 |
| request config host `app.box.com:443` + basePath | `box.base-url`, `box.base-path` | `https://app.box.com`, `/api/oauth2/authorize` |
| `tokenUrl` | `box.token-url` | `https://app.box.com/api/oauth2/token` |
| request config `tfim01.demos.ibm.com:443` | `fim.base-url` | `https://tfim01.demos.ibm.com` |
| `truststore.*`, `keystore.*` | JVM / Spring SSL bundle | see deltas |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-oauth-calls-box-with-client-credentials-token` | `/oauth` → `GET /api/oauth2/authorize/` with `Bearer` startup token, Box body returned | documented |
| `02-oauth-box-error-is-500` | Box 404 → 500 `Response code 404 mapped as failure.` | documented |
| `03-fim-proxy-get-forwards-path-and-query` | `/FIM/*` GET keeps method, path, query | documented |
| `04-fim-proxy-post-forwards-body` | `/FIM/*` POST keeps body and `Content-Type` | documented |
| `05-fim-proxy-error-is-500` | FIM 503 → 500 `Response code 503 mapped as failure.` | documented |
| `ClientCredentialsGrantFlowTest` (3 tests) | startup token request (Basic, `grant_type=client_credentials`), 401 → refresh → resend once, second 401 fails | documented (Mule 3.8 `ClientCredentialsGrantType`, `ClientCredentialsTokenRequestHandler`) |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas): the default JVM trust store replaces `TLS_Context`.
