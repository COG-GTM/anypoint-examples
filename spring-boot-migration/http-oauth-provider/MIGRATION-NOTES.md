# Migration notes: `http-oauth-provider`

- Original Mule app: [`../../http-oauth-provider`](../../http-oauth-provider)
- Mule flows: `http-oauth-provider.xml` → `protectedAuthcodeFlow`, `redirectFlow` (plus the endpoints of `oauth2-provider:config`)
- Spring Boot module: `spring-boot-migration/http-oauth-provider`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `ss:authentication-manager` / `ss:user-service` (`mule`/`mule`, `RESOURCE_OWNER`) | `AuthorizationServerConfig#resourceOwners` (`InMemoryUserDetailsManager`) | |
| `mule-ss:security-manager` + `delegate-security-provider resourceOwnerSecurityProvider` | `DaoAuthenticationProvider` in `ProviderManager`, used by `LoginOnAuthorizeFilter` | |
| `oauth2-provider:config port=${http.provider.port}` | Spring Authorization Server (`authorizationServerChain`) on a second Tomcat connector (`ProviderConnectorConfig`) | |
| `oauth2-provider:config providerName="SampleAPI"` login page | `LoginPage` (rendered by the authentication entry point for `/authorize`) | |
| authorization endpoint `/authorize` | `AuthorizationServerSettings.authorizationEndpoint("/authorize")` + `LoginOnAuthorizeFilter` (login form posts back to `/authorize`) | Consent not required, as in Mule. |
| token endpoint `/token` (GET query or POST form) | `AuthorizationServerSettings.tokenEndpoint("/token")` + `LegacyTokenRequestFilter` (GET → form POST) | |
| token response | `AuthorizationServerConfig#muleTokenResponse` (`scope`, `expires_in`, `token_type: bearer`, `access_token`) | |
| `oauth2-provider:config scopes="READ_RESOURCE POST_RESOURCE"`, client `scopes` | `RegisteredClient.scopes` + `DEFAULT_SCOPE_VALIDATOR` | |
| `oauth2-provider:client` (`myclientid`, `myclientsecret`, `Mule Bookstore`, `CONFIDENTIAL`) | `RegisteredClient` (`CLIENT_SECRET_POST`, `CLIENT_SECRET_BASIC`) in `InMemoryRegisteredClientRepository` | |
| `authorized-grant-type AUTHORIZATION_CODE` | `AuthorizationGrantType.AUTHORIZATION_CODE` only, `requireProofKey(false)` | |
| `redirect-uri http://localhost*` | `AuthorizationServerConfig#muleRedirectUriValidator` (`*` wildcard) | |
| in-memory token store, default token TTL 86400 s, opaque tokens | `InMemoryOAuth2AuthorizationService`, `OAuth2TokenFormat.REFERENCE`, `accessTokenTimeToLive` 86400 s | |
| `http:listener-config HTTP_Listener_Configuration` (`${http.listener.port}`) | embedded Tomcat main connector, `server.port=${http.listener.port}` | |
| `oauth2-provider:validate scopes=READ_RESOURCE throwExceptionOnUnaccepted=true` | `resourceServerChain` (`hasAuthority("SCOPE_READ_RESOURCE")`, `oauth2ResourceServer().opaqueToken()`) + `localIntrospector` | |
| `set-payload` map + `json:object-to-json-transformer` (`protectedAuthcodeFlow`) | `ResourceController#resources` (`Map` → Jackson) | |
| `set-property http.status=302` + `Location` (`redirectFlow`) | `ResourceController#redirect` | Literal `&&` kept. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.provider.port` | `http.provider.port` (provider connector) | 8081 |
| `http.listener.port` | `http.listener.port` → `server.port` | 8082 |
| `providerName` | `provider.name` | `SampleAPI` |
| client id / secret / name / scopes / redirect URIs | `provider.client.*` | as in the flow XML |
| resource owner | `provider.resource-owner.*` | `mule` / `mule` |
| token TTL | `provider.token-ttl-seconds` | 86400 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-authorize-shows-login-page` | `/authorize` shows the SampleAPI login form with `username`/`password` ids | documented |
| `02-login-redirects-with-authorization-code` | `mule`/`mule` login → 302 to `http://localhost:8082/redirect?code=...` | documented |
| `03-redirect-builds-legacy-token-url` | `redirectFlow` `Location`, including `&&` | documented |
| `04-resources-without-token-is-401` | no bearer token → 401 | documented |
| `05-resources-with-unknown-token-is-401` | unknown bearer token → 401 | documented |
| `http-oauth-provider-flow/token-exchange` | GET `/token` response shape (`scope`, `expires_in` 86400, `token_type` bearer, `access_token`) | documented |
| `http-oauth-provider-flow/token-exchange-post` | same shape for the POST form | documented |
| `http-oauth-provider-flow/protected-resource` | `/resources` with the issued token → payroll JSON | documented |
| `ProviderGrantFlowTest` (4 tests) | the full dance, single-use codes, wrong password, redirect URI pattern | documented (Mule README, `HttpOauthProviderIT`) |
| `ProviderConnectorTest` | endpoints stay on their Mule ports | documented (flow XML) |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas): login page markup, Spring Authorization Server error responses, token value format,
extra standard endpoints, and Basic client authentication also accepted.
