# Migration notes: `import-leads-into-salesforce`

- Original Mule app: [`../../import-leads-into-salesforce`](../../import-leads-into-salesforce)
- Mule flows: `import-leads-into-salesforce.xml` → batch job `CreateLeadsBatch`
- Spring Boot module: `spring-boot-migration/import-leads-into-salesforce`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `batch:job name=CreateLeadsBatch max-failed-records=1000` | `CreateLeadsBatchConfig#createLeadsBatch` (Spring Batch `Job`), `faultTolerant().skip(Exception).skipLimit(1000)` |  |
| `batch:input` → `file:inbound-endpoint` (10 s, move to output) | `LeadsFileTrigger#pollCsvFiles` (Spring Integration file adapter → `JobLauncher`) | One job run per file |
| `dw:transform-message` "Transform CSV to Maps" | `LeadCsv#toLeads` + step-scoped `ListItemReader` | Birthday column dropped |
| `LeadExistsStep`: `enricher` + `sfdc:query "SELECT Id FROM Lead WHERE Email = '...'"` | `CreateLeadsBatchConfig#leadExistsStep` (`ItemProcessor`) | Returns `null` (filtered) when the lead exists = `accept-expression="#[!recordVars['exists']]"` |
| `LeadInsertStep`: `logger` + `batch:commit size=200` + `sfdc:create type=Lead` | Chunk size 200 + `CreateLeadsBatchConfig#insertLeads` (`ItemWriter`) |  |
| `LogFailuresStep accept-policy=ONLY_FAILURES` | `SkipListener` logging `Got Failure {...}` |  |
| `batch:on-complete` logger | `JobExecutionListener#afterJob` logging `N Loaded Records M Failed Records` | loaded = read count, failed = skip count |
| `sfdc:config` (username / password / securityToken, connection pooling) | `salesforce.SalesforcePasswordSession` (OAuth2 username-password flow, cached session) + `SalesforceConfig` | Re-login once on 401 |
| Salesforce connector operations | `salesforce.SalesforceClient` (Spring `RestClient`) | Typed REST client; queries follow `nextRecordsUrl` |
| Connector record maps | `salesforce.SalesforceRecords#toMuleMap` | `HashMap` + `type`, SOAP date format |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `file:inbound-endpoint path` | `file.input-path` | `src/main/resources/input` |
| `moveToDirectory` | `file.output-path` | `src/main/resources/output` |
| `pollingFrequency` | `file.polling-frequency` (ms) | `10000` |
| `sfdc.user` | `sfdc.user` | blank |
| – | `spring.datasource.url` (Spring Batch job repository) | in-memory H2 |
| `sfdc.password` | `sfdc.password` | blank |
| `sfdc.securityToken` | `sfdc.securityToken` | blank |
| – | `sfdc.consumerKey` / `sfdc.consumerSecret` (new: Connected App) | blank |
| `sfdc:config url` (SOAP login endpoint) | `sfdc.login-url` | `https://login.salesforce.com` |
| – | `sfdc.api-version` | `61.0` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-new-leads-are-created` | Sample leads.csv (CR line endings, extra Birthday column dropped by the mapping): neither lead exists, so both are looked up by Email and then created in one commit (one sObject create call). | documented |
| `02-existing-lead-is-skipped` | The first lead already exists (query returns a record), so LeadInsertStep does not accept it: only the second lead is created. | documented |
| `03-all-leads-exist-no-create` | Both leads already exist: they are looked up but no create call is made. | documented |
| `04-failed-lookup-fails-only-that-record` | The lookup for the first lead fails (Salesforce 500). | documented |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas).
