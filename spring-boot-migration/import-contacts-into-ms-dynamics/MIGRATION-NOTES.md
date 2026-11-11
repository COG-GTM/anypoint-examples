# Migration notes: `import-contacts-into-ms-dynamics`

- Original Mule app: [`../../import-contacts-into-ms-dynamics`](../../import-contacts-into-ms-dynamics)
- Mule flows: `import-contacts-into-ms-dynamics.xml` → `crm-create-account`
- Spring Boot module: `spring-boot-migration/import-contacts-into-ms-dynamics`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `file:inbound-endpoint path="src/main/resources/input" pollingFrequency="10000"` | `ContactImportFlowConfig#crmCreateAccount` (`Files.inboundAdapter` + `Pollers.fixedDelay`) | Spring Integration. |
| `moveToDirectory="src/main/resources/output"` | `ContactImporter#moveToOutput` (`Files.move`, after reading) | |
| `mimeType="application/csv"` + `dw:transform-message` "CSV To Map" | `ContactImporter#read` (Jackson `CsvMapper`, header row) + `ContactRecord#from` | Columns matched by header name. |
| `foreach` | `for` loop in `ContactImporter#importFile` | Sequential, row order. |
| `dynamicscrm:config` (Legacy Connection, `CRMONLINE`) | `CrmProperties` + `DynamicsCrmClient` (`WebClient`, bearer token, OData headers) | Delta 1. |
| `dynamicscrm:create logicalName="contact"` + `dynamicscrm:attribute` ×4 | `DynamicsCrmClient#createContact(DynamicsContact)` → `POST /api/data/v9.2/contacts` | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `crm.service-url` | `crm.base-url` | `https://yourorg.crm.dynamics.com` |
| `crm.username` / `crm.password` / `crm.auth-type` | `crm.access-token` (OAuth 2.0) | empty |
| (file endpoint `path`) | `contacts.input-dir` | `src/main/resources/input` |
| (file endpoint `moveToDirectory`) | `contacts.output-dir` | `src/main/resources/output` |
| (file endpoint `pollingFrequency`) | `contacts.polling-frequency` | `10000` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-sample-contacts-file` | bundled `contacts.csv` → two creates with the documented attribute mapping, in order; file moved | documented |
| `02-quoted-csv-fields` | RFC 4180 quoting and header-name column matching | documented |
| `03-header-only-file` | no rows → no Dynamics calls; file still moved | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas).
