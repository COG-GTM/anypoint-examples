# import-contacts-into-salesforce (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`import-contacts-into-salesforce`](../../import-contacts-into-salesforce). It polls a folder for CSV files of contacts, moves each file to an output folder and creates the contacts in Salesforce.

## Run

```bash
mvn -pl import-contacts-into-salesforce -am spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dsfdc.user=... -Dsfdc.password=... -Dsfdc.securityToken=... -Dsfdc.consumerKey=... -Dsfdc.consumerSecret=..."
# then drop a CSV (header: firstname,surname,phone,email) into src/main/resources/input
```

## Tests

```bash
cd spring-boot-migration
mvn -pl import-contacts-into-salesforce -am verify
```

`ContactsToSfdcParityTest` drops each fixture's CSV into a temporary input folder. It then asserts the exact Salesforce calls through WireMock (token request plus the create payload) and the input and output folder contents.

Fixtures are `documented` evidence. Replaying them against a live Mule runtime (`-Dparity.mule.base-url`) would need a real Salesforce org behind the Mule app, so CI compares the Spring module with the documented Mule behaviour while WireMock stands in for the Salesforce API.

## Behavioural deltas

- **Salesforce login.** The Mule connector logged in through the SOAP partner API with username, password and security token. The Spring module uses the OAuth2 username-password flow (`POST ${sfdc.login-url}/services/oauth2/token`). That flow also needs a Connected App, so the new properties `sfdc.consumerKey` and `sfdc.consumerSecret` are required. The password + security token concatenation is unchanged. The session is cached and re-established once on HTTP 401, the way the connector's pool reconnected.
- **API.** SOAP partner API calls become REST API calls (`sfdc.api-version`, default `61.0`). Records are converted back to the connector's shape (a `HashMap` of fields plus `type`, with date-times in SOAP format `2014-08-25T13:21:00.000Z` instead of REST's `+0000`), so logs and watermarks look the same.
- **Create.** `sfdc:create` becomes sObject Collections (`POST /composite/sobjects`, `allOrNone=false`, at most 200 records per call). Per-record success and failure is reported the same way as the SOAP `create`. Lists longer than 200 are split into several calls, where a single SOAP `create` call is limited to 200 objects.
- **File move timing.** The file is read and moved to the output folder before Salesforce is called. Mule's streaming file connector moved it once the stream had been consumed. In both apps the file ends up in the output folder, even when the Salesforce call fails.
