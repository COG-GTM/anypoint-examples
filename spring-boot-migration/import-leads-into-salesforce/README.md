# import-leads-into-salesforce (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`import-leads-into-salesforce`](../../import-leads-into-salesforce). It polls a folder for CSV files of leads and runs a batch job that looks each lead up by email and creates the ones that don't exist yet.

## Run

```bash
mvn -pl import-leads-into-salesforce -am spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dsfdc.user=... -Dsfdc.password=... -Dsfdc.securityToken=... -Dsfdc.consumerKey=... -Dsfdc.consumerSecret=..."
# then drop a CSV (header: Company,FirstName,LastName,Birthday,Email) into src/main/resources/input
```

## Tests

```bash
cd spring-boot-migration
mvn -pl import-leads-into-salesforce -am verify
```

`CreateLeadsBatchParityTest` drops each fixture's CSV (including the original `leads.csv`, which has CR line endings) into a temporary input folder. It then asserts the exact per-lead SOQL lookups and the create payload through WireMock, and the job's loaded and failed counters from the Spring Batch `JobExplorer`.

Fixtures are `documented` evidence. Replaying them against a live Mule runtime (`-Dparity.mule.base-url`) would need a real Salesforce org behind the Mule app, so CI compares the Spring module with the documented Mule behaviour while WireMock stands in for the Salesforce API.

## Behavioural deltas

- **Salesforce login.** The Mule connector logged in through the SOAP partner API with username, password and security token. The Spring module uses the OAuth2 username-password flow (`POST ${sfdc.login-url}/services/oauth2/token`). That flow also needs a Connected App, so the new properties `sfdc.consumerKey` and `sfdc.consumerSecret` are required. The password + security token concatenation is unchanged. The session is cached and re-established once on HTTP 401, the way the connector's pool reconnected.
- **API.** SOAP partner API calls become REST API calls (`sfdc.api-version`, default `61.0`). Records are converted back to the connector's shape (a `HashMap` of fields plus `type`, with date-times in SOAP format `2014-08-25T13:21:00.000Z` instead of REST's `+0000`), so logs and watermarks look the same.
- **Create.** `sfdc:create` becomes sObject Collections (`POST /composite/sobjects`, `allOrNone=false`, at most 200 records per call). Per-record success and failure is reported the same way as the SOAP `create`. Lists longer than 200 are split into several calls, where a single SOAP `create` call is limited to 200 objects.
- **Batch engine.** Mule's three batch steps become one chunk-oriented Spring Batch step: processor = exists check plus accept-expression, writer = commit of 200, skip listener = `ONLY_FAILURES` step. Mule ran the job asynchronously on its own thread pool and kept the batch state in its queue store. Spring runs it synchronously on the poller thread and keeps job metadata in an in-memory H2 job repository. The records created and the loaded and failed counts are the same.
- **SOQL built by concatenation.** The email is inserted into the SOQL verbatim, exactly as in the Mule flow, so an email containing `'` makes that record's lookup fail, and it is counted as a failed record in both apps.
