# authenticating-salesforce-using-oauth2 (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`authenticating-salesforce-using-oauth2`](../../authenticating-salesforce-using-oauth2). `GET /` makes sure the app is authorized with Salesforce through the OAuth2 web-server (authorization code) flow, queries 10 contacts, logs each one and answers `Salesforce query returned N contacts.`

## Run

1. Create a Connected App with callback URL `http://localhost:8081/oauth2callback` (README of the Mule example).
2. Run:

```bash
mvn -pl authenticating-salesforce-using-oauth2 -am spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dsfdc.key=<consumer key> -Dsfdc.secret=<consumer secret>"
open http://localhost:8081/
```

## Tests

```bash
cd spring-boot-migration
mvn -pl authenticating-salesforce-using-oauth2 -am verify
```

`SalesforceOAuthParityTest` (an `AbstractWireMockParityTest`) replays the fixtures, using `given.authorized` to say whether a token is already stored. It asserts the redirect, the token exchange form, the SOQL and the response text. An extra test covers token refresh on 401.

Fixtures are `documented` evidence. Replaying them against a live Mule runtime (`-Dparity.mule.base-url`) would need a real Salesforce org behind the Mule app, so CI compares the Spring module with the documented Mule behaviour while WireMock stands in for the Salesforce API.

## Behavioural deltas

- **API.** SOAP partner API calls become REST API calls (`sfdc.api-version`, default `61.0`). Records are converted back to the connector's shape (a `HashMap` of fields plus `type`, with date-times in SOAP format `2014-08-25T13:21:00.000Z` instead of REST's `+0000`), so logs and watermarks look the same.
- **Callback listener.** Mule opened a separate callback listener on `localPort` (8081). The Spring module serves `/oauth2callback` on its own `server.port`. `sfdc.oauth.callback.*` only builds the `redirect_uri`, so keep it pointing at this app.
- **`state` parameter.** Mule put its suspended event id into `state` so it could resume the flow. The Spring module sends a single-use random `state` and answers 403 `Salesforce authorization failed: unknown OAuth state` to a callback that doesn't return one it issued, so a forged authorization code can't replace the stored session (fixture `05-...`). Fixtures match the redirect by parameter (`response_type`, `client_id`, `redirect_uri`, `display`, `state`).
- **Callback without a code** (for example when the user denies access) answers 500 with `Salesforce authorization failed: <error> <error_description>`. The Mule example doesn't document this case.
- **Token storage** is in memory, so a restart needs a new authorization, the same as the connector's default in-memory object store.
