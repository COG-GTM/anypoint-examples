# salesforce-data-retrieval (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`salesforce-data-retrieval`](../../salesforce-data-retrieval). `GET /` shows an HTML form listing the org's sObject types; `POST /` builds a SOQL query from the form and returns the matching records.

## Run

```bash
mvn -pl salesforce-data-retrieval -am spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dhttp.port=8081 -Dsfdc.username=... -Dsfdc.password=... -Dsfdc.securityToken=... -Dsfdc.consumerKey=... -Dsfdc.consumerSecret=..."
open http://localhost:8081/
```

## Tests

```bash
cd spring-boot-migration
mvn -pl salesforce-data-retrieval -am verify
```

`SalesforceRetrievalParityTest` (an `AbstractWireMockParityTest`) replays each fixture over HTTP. It asserts the exact HTML or text response and the exact SOQL sent to `queryAll`.

Fixtures are `documented` evidence. Replaying them against a live Mule runtime (`-Dparity.mule.base-url`) would need a real Salesforce org behind the Mule app, so CI compares the Spring module with the documented Mule behaviour while WireMock stands in for the Salesforce API.

## Behavioural deltas

- **Salesforce login.** The Mule connector logged in through the SOAP partner API with username, password and security token. The Spring module uses the OAuth2 username-password flow (`POST ${sfdc.login-url}/services/oauth2/token`). That flow also needs a Connected App, so the new properties `sfdc.consumerKey` and `sfdc.consumerSecret` are required. The password + security token concatenation is unchanged. The session is cached and re-established once on HTTP 401, the way the connector's pool reconnected.
- **API.** SOAP partner API calls become REST API calls (`sfdc.api-version`, default `61.0`). Records are converted back to the connector's shape (a `HashMap` of fields plus `type`, with date-times in SOAP format `2014-08-25T13:21:00.000Z` instead of REST's `+0000`), so logs and watermarks look the same.
- **Content-Type.** `GET /` answers `text/html;charset=UTF-8` and `POST /` answers `text/plain;charset=UTF-8`. Mule derived the header from the payload's data type, which the example does not document, so fixtures don't assert it.
- **Null field values.** Fields with a null value are printed as `Field=null`. Mule's `MapUtils.toString` would throw on a null value (answered as `Invalid Salesforce query.`) if the connector put one in the map. Whether it did is not documented.
