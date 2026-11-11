# Migration notes: `salesforce-data-synchronization-using-watermarking-and-batch-processing`

- Original Mule app: [`../../salesforce-data-synchronization-using-watermarking-and-batch-processing`](../../salesforce-data-synchronization-using-watermarking-and-batch-processing)
- Mule flows: `watermarking.xml` → `triggerFlow`, batch job `watermarkingBatch`
- Spring Boot module: `spring-boot-migration/salesforce-data-synchronization-using-watermarking-and-batch-processing`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `poll` + `fixed-frequency-scheduler frequency=10 timeUnit=SECONDS` (`triggerFlow`) | `PollScheduler#poll` (`@Scheduled(fixedRate = 10 s, initialDelay = 0)`) → `TriggerFlow#poll` | `poll.enabled=false` turns the schedule off |
| `watermark variable=timestamp default-expression=YESTERDAY` | `WatermarkStore` (JDBC table `watermark`) + `poll.watermark.default-expression` | The `YESTERDAY` SOQL literal is used verbatim on the first run |
| `watermark selector=MAX selector-expression=#[payload.LastModifiedDate]` | `SalesforceQueryItemReader` (MAX over every returned record) → `TriggerFlow` persists it | Not advanced when nothing came back or the query failed |
| `sfdc:query "SELECT Id,Name,LastModifiedDate FROM Contact WHERE LastModifiedDate > #[flowVars['timestamp']]"` | Step-scoped `SalesforceQueryItemReader` (`WatermarkingBatchConfig#recentlyModifiedContacts`) | Follows `nextRecordsUrl` pages |
| `batch:execute` / `batch:job name=watermarkingBatch` | `WatermarkingBatchConfig#watermarkingBatch` (Spring Batch `Job`), chunk 100 (Mule block size) |  |
| `Batch_Step` → `logger "Contact recently modified: #[payload]"` | `WatermarkingBatchConfig#logEachContact` (`ItemWriter`) | Same text and map order |
| `sfdc:config` (username / password / securityToken, connection pooling) | `salesforce.SalesforcePasswordSession` (OAuth2 username-password flow, cached session) + `SalesforceConfig` | Re-login once on 401 |
| Salesforce connector operations | `salesforce.SalesforceClient` (Spring `RestClient`) | Typed REST client; queries follow `nextRecordsUrl` |
| Connector record maps | `salesforce.SalesforceRecords#toMuleMap` | `HashMap` + `type`, SOAP date format |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `fixed-frequency-scheduler frequency` | `poll.frequency` (ms) | `10000` |
| `watermark default-expression` | `poll.watermark.default-expression` | `YESTERDAY` |
| – | `spring.datasource.url` (watermark + job repository) | `jdbc:h2:file:./data/salesforce-watermarking` |
| `sfdc.user` | `sfdc.user` | blank |
| `sfdc.password` | `sfdc.password` | blank |
| `sfdc.securityToken` | `sfdc.securityToken` | blank |
| – | `sfdc.consumerKey` / `sfdc.consumerSecret` (new: Connected App) | blank |
| `sfdc:config url` (SOAP login endpoint) | `sfdc.login-url` | `https://login.salesforce.com` |
| – | `sfdc.api-version` | `61.0` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-first-poll-uses-default-and-advances-to-max` | First run: no stored watermark, so the query uses the default expression YESTERDAY (a SOQL date literal). | documented |
| `02-next-poll-uses-stored-watermark` | A later poll queries with the stored watermark value inserted verbatim (LastModifiedDate > 2015-06-17T11:30:15.000Z) and advances it to the newest LastModifiedDate returned. | documented |
| `03-no-changes-keeps-watermark` | Nothing modified since the watermark: no records, nothing logged, and the MAX selector has no value so the watermark is unchanged. | documented |
| `04-failed-query-keeps-watermark` | The poll's query fails (Salesforce 500): the flow fails before the batch runs, so nothing is logged and the watermark is not updated; the next poll retries from the same value. | documented |
| `05-paged-result-takes-max-over-all-pages` | A result split over two pages (nextRecordsUrl, as the connector's paging iterator follows): every record from both pages is processed and the watermark is the MAX over all of them, not just the first page. | documented |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas).
