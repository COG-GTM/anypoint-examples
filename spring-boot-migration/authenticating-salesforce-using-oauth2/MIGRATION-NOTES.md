# Migration notes: `authenticating-salesforce-using-oauth2`

- Original Mule app: [`../../authenticating-salesforce-using-oauth2`](../../authenticating-salesforce-using-oauth2)
- Mule flows: `salesforce-oauth.xml` → `salesforce-oauthFlow1`
- Spring Boot module: `spring-boot-migration/authenticating-salesforce-using-oauth2`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `sfdc:config-with-oauth consumerKey consumerSecret` | `SalesforceOAuth` (`sfdc.key`, `sfdc.secret`) | In-memory token store, like the connector's default |
| `sfdc:oauth-callback-config domain=localhost localPort=8081 path=oauth2callback` | `SalesforceOAuthController#callback` (`@GetMapping("/oauth2callback")`) + `sfdc.oauth.callback.*` (redirect URI) | Served on `server.port` |
| `sfdc:authorize authorizationUrl accessTokenUrl display=PAGE` | `SalesforceOAuthController#root` → 302 to `SalesforceOAuth#authorizationRedirect`; `SalesforceOAuth#exchange` (authorization_code grant) | After the callback the rest of the flow runs and its result is the callback response |
| Connector access-token refresh on expired session | `SalesforceOAuth#renew` (refresh_token grant) called by `SalesforceClient` on 401 | Covered by `SalesforceOAuthParityTest#expiredAccessTokenIsRefreshedAndQueryRetried` |
| `sfdc:query "SELECT id,lastname,lastmodifieddate from contact limit 10"` | `SalesforceClient#query` → `GET /query?q=...` |  |
| `foreach` + `logger "contact: #[payload]"` | SLF4J `contact: {LastModifiedDate=..., Id=..., LastName=..., type=Contact}` | Identical to the README's sample log lines |
| `set-payload "Salesforce query returned #[payload.size()] contacts."` | Controller return value |  |
| Connector record maps | `salesforce.SalesforceRecords#toMuleMap` |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `sfdc.key` | `sfdc.key` | blank |
| `sfdc.secret` | `sfdc.secret` | blank |
| `authorizationUrl` | `sfdc.oauth.authorization-url` | `https://login.salesforce.com/services/oauth2/authorize` |
| `accessTokenUrl` | `sfdc.oauth.access-token-url` | `https://login.salesforce.com/services/oauth2/token` |
| `display` | `sfdc.oauth.display` | `PAGE` |
| `oauth-callback-config domain / localPort / path` | `sfdc.oauth.callback.domain / local-port / path` | `localhost` / `8081` / `oauth2callback` |
| – | `sfdc.api-version` | `61.0` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-unauthorized-request-redirects-to-salesforce` | With no access token stored, sfdc:authorize redirects the browser (302) to the authorization URL with response_type=code, the Connected App's consumer key, the callback redirect_uri built from oauth-callback-config (http://localhost:8081/oauth2callback) and display=page; nothing is sent to Salesforce's API. | documented |
| `02-callback-exchanges-code-and-runs-query` | Salesforce redirects back to /oauth2callback with the authorization code: the code is exchanged at accessTokenUrl (authorization_code grant with consumer key/secret and the same redirect_uri), then the rest of the flow runs with the new token: the fixed SOQL query and 'Salesforce query returned N contacts.' (SalesforceOauthIT expects the page to contain 'Salesforce query returned'). | documented |
| `03-authorized-request-queries-directly` | Once a token is stored, sfdc:authorize does not redirect: GET / runs the query with the stored access token straight away (no token request) and returns the count. | documented |
| `04-authorized-request-with-no-contacts` | An org without contacts: the query returns no records and the flow answers with a zero count. | documented |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas).
