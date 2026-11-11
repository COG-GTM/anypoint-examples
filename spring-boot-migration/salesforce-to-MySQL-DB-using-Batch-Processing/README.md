# salesforce-to-MySQL-DB-using-Batch-Processing (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`salesforce-to-MySQL-DB-using-Batch-Processing`](../../salesforce-to-MySQL-DB-using-Batch-Processing). Every 60 seconds it queries Salesforce for contacts modified since a persisted watermark and inserts or updates them, matched by email, in the MySQL table `contact`.

## Run

Create the `company` database and the `contact` table from [`src/main/resources/contact.sql`](src/main/resources/contact.sql), as for the Mule example, then:

```bash
mvn -pl salesforce-to-MySQL-DB-using-Batch-Processing -am spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dmysql.host=localhost -Dmysql.user=... -Dmysql.password=... -Dsfdc.user=... -Dsfdc.password=... -Dsfdc.securityToken=... -Dsfdc.consumerKey=... -Dsfdc.consumerSecret=..."
```

## Tests

```bash
cd spring-boot-migration
mvn -pl salesforce-to-MySQL-DB-using-Batch-Processing -am verify
```

`SalesforceToMySqlParityTest` runs against a real MySQL 5.7 (Testcontainers `ParityContainers.mysql()`, schema from the Mule app's `contact.sql`) and the WireMock Salesforce API. Per fixture it seeds the clock, the watermark and the `contact` rows, runs `triggerFlow` once, then asserts the exact SOQL, the `contact` table and the watermark.

Fixtures are `documented` evidence. Replaying them against a live Mule runtime (`-Dparity.mule.base-url`) would need a real Salesforce org behind the Mule app, so CI compares the Spring module with the documented Mule behaviour while WireMock stands in for the Salesforce API.

## Behavioural deltas

- **Salesforce login.** The Mule connector logged in through the SOAP partner API with username, password and security token. The Spring module uses the OAuth2 username-password flow (`POST ${sfdc.login-url}/services/oauth2/token`). That flow also needs a Connected App, so the new properties `sfdc.consumerKey` and `sfdc.consumerSecret` are required. The password + security token concatenation is unchanged. The session is cached and re-established once on HTTP 401, the way the connector's pool reconnected.
- **API.** SOAP partner API calls become REST API calls (`sfdc.api-version`, default `61.0`). Records are converted back to the connector's shape (a `HashMap` of fields plus `type`, with date-times in SOAP format `2014-08-25T13:21:00.000Z` instead of REST's `+0000`), so logs and watermarks look the same.
- **Watermark storage.** Mule kept the watermark in its persistent object store. The Spring module keeps it in a `watermark` table in the same MySQL schema, created on startup if missing. Spring Batch's job repository tables are created in that schema too (`spring.batch.jdbc.initialize-schema=always`).
- **Batch execution** is synchronous inside each poll, where Mule's `batch:execute` was asynchronous. The rows written and the watermark advancement are the same.
