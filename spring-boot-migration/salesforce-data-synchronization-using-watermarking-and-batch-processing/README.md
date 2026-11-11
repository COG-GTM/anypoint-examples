# salesforce-data-synchronization-using-watermarking-and-batch-processing (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`salesforce-data-synchronization-using-watermarking-and-batch-processing`](../../salesforce-data-synchronization-using-watermarking-and-batch-processing). Every 10 seconds it queries Salesforce for contacts modified since a persisted watermark, logs each one from a batch step and advances the watermark to the newest `LastModifiedDate`.

## Run

```bash
mvn -pl salesforce-data-synchronization-using-watermarking-and-batch-processing -am spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dsfdc.user=... -Dsfdc.password=... -Dsfdc.securityToken=... -Dsfdc.consumerKey=... -Dsfdc.consumerSecret=..."
```

The watermark is kept in `./data/salesforce-watermarking` (H2 file), table `watermark`.

## Tests

```bash
cd spring-boot-migration
mvn -pl salesforce-data-synchronization-using-watermarking-and-batch-processing -am verify
```

`WatermarkingParityTest` runs `triggerFlow` once per fixture after seeding the stored watermark from `given.watermark`. It asserts the exact SOQL (including the watermark value) through WireMock, the watermark afterwards, and the batch step's log lines.

Fixtures are `documented` evidence. Replaying them against a live Mule runtime (`-Dparity.mule.base-url`) would need a real Salesforce org behind the Mule app, so CI compares the Spring module with the documented Mule behaviour while WireMock stands in for the Salesforce API.

## Behavioural deltas

- **Salesforce login.** The Mule connector logged in through the SOAP partner API with username, password and security token. The Spring module uses the OAuth2 username-password flow (`POST ${sfdc.login-url}/services/oauth2/token`). That flow also needs a Connected App, so the new properties `sfdc.consumerKey` and `sfdc.consumerSecret` are required. The password + security token concatenation is unchanged. The session is cached and re-established once on HTTP 401, the way the connector's pool reconnected.
- **API.** SOAP partner API calls become REST API calls (`sfdc.api-version`, default `61.0`). Records are converted back to the connector's shape (a `HashMap` of fields plus `type`, with date-times in SOAP format `2014-08-25T13:21:00.000Z` instead of REST's `+0000`), so logs and watermarks look the same.
- **Watermark storage.** Mule kept the watermark in its persistent object store. The Spring module keeps it in table `watermark` (row `triggerFlow.timestamp`) in an H2 file database, so it survives restarts in the same way. Spring Batch job metadata lives in the same database.
- **Batch execution** is synchronous inside each poll. Mule's `batch:execute` queued the job and returned. Records processed, log output and watermark advancement are the same.
