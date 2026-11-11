# Migration notes: `salesforce-to-MySQL-DB-using-Batch-Processing`

- Original Mule app: [`../../salesforce-to-MySQL-DB-using-Batch-Processing`](../../salesforce-to-MySQL-DB-using-Batch-Processing)
- Mule flows: `salesforce-to-database.xml` → `triggerFlow`, batch job `salesforce-to-database-Batch`
- Spring Boot module: `spring-boot-migration/salesforce-to-MySQL-DB-using-Batch-Processing`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `poll` + `fixed-frequency-scheduler frequency=60 timeUnit=SECONDS` (`triggerFlow`) | `PollScheduler#poll` (`@Scheduled(fixedRate = 60 s)`) → `TriggerFlow#poll` |  |
| `watermark default-expression=#[groovy: now - 10000 ms formatted yyyy-MM-dd'T'HH:mm:ss.SSS'Z' UTC]` | `TriggerFlow` (`Clock` minus `poll.watermark.default-look-back`, same format) |  |
| `watermark selector=MAX selector-expression=#[payload.LastModifiedDate]` | `SalesforceQueryItemReader` + `WatermarkStore` (table `watermark` in MySQL) |  |
| `sfdc:query "SELECT Email,FirstName,LastModifiedDate,LastName FROM Contact WHERE LastModifiedDate > #[timestamp]"` | Step-scoped `SalesforceQueryItemReader` (`SalesforceToDatabaseBatchConfig#modifiedContacts`) |  |
| `logger "#[flowVars.timestamp]"` | `log.info(timestamp)` in `TriggerFlow#poll` |  |
| `batch:job name=salesforce-to-database-Batch` | `SalesforceToDatabaseBatchConfig#salesforceToDatabaseBatch` (Spring Batch), chunk 100 | Not fault-tolerant: Mule's default `max-failed-records` is 0 |
| `queryExistingContactInDbStep`: `dw:transform-message` (Email/FirstName/LastName/LastModifiedDate → email/first_name/last_name/last_modified) + `enricher` `db:select ... WHERE email=?` | `SalesforceToDatabaseBatchConfig#queryExistingContactInDb` (`ItemProcessor` → `DbContact`) | `JdbcTemplate` |
| `upsertContactInDbStep` `choice`: `db:insert` / `db:update` | `SalesforceToDatabaseBatchConfig#upsertContactInDb` (`ItemWriter`) | Same parameterized SQL |
| `db:mysql-config database=company host port=3306 user password` | `spring.datasource.*` (`jdbc:mysql://${mysql.host}:3306/company`) |  |
| `batch:on-complete` → `logger "Batch sf->db has finished"` | `JobExecutionListener#afterJob` |  |
| `sfdc:config` (username / password / securityToken, connection pooling) | `salesforce.SalesforcePasswordSession` (OAuth2 username-password flow, cached session) + `SalesforceConfig` | Re-login once on 401 |
| Salesforce connector operations | `salesforce.SalesforceClient` (Spring `RestClient`) | Typed REST client; queries follow `nextRecordsUrl` |
| Connector record maps | `salesforce.SalesforceRecords#toMuleMap` | `HashMap` + `type`, SOAP date format |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `mysql.host` | `mysql.host` | `localhost` |
| `mysql.user` | `mysql.user` | blank |
| `mysql.password` | `mysql.password` | blank |
| `fixed-frequency-scheduler frequency` | `poll.frequency` (ms) | `60000` |
| `watermark default-expression` (now - 10 s) | `poll.watermark.default-look-back` | `10s` |
| `sfdc.user` | `sfdc.user` | blank |
| `sfdc.password` | `sfdc.password` | blank |
| `sfdc.securityToken` | `sfdc.securityToken` | blank |
| – | `sfdc.consumerKey` / `sfdc.consumerSecret` (new: Connected App) | blank |
| `sfdc:config url` (SOAP login endpoint) | `sfdc.login-url` | `https://login.salesforce.com` |
| – | `sfdc.api-version` | `61.0` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-first-poll-inserts-new-contacts` | First run (no stored watermark) at 2015-10-10T10:00:10.123Z: the default watermark is now minus 10 s in UTC ISO format (2015-10-10T10:00:00.123Z). | documented |
| `02-existing-email-is-updated-new-one-inserted` | With a stored watermark the query uses it verbatim. | documented |
| `03-no-changes-leaves-table-and-watermark` | No contacts modified since the watermark: the table is not touched and the watermark keeps its value. | documented |
| `04-failed-query-changes-nothing` | The poll's Salesforce query fails: the batch does not run, the table is unchanged and the watermark is not advanced. | documented |
| `05-email-match-is-case-insensitive` | contact.sql declares the table with COLLATE utf8_slovak_ci, so the existence check matches an email that differs only in case: the row is UPDATEd (keeping the stored email) rather than a duplicate being INSERTed. | documented |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas).
