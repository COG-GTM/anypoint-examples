# Migration notes: `import-contacts-into-salesforce`

- Original Mule app: [`../../import-contacts-into-salesforce`](../../import-contacts-into-salesforce)
- Mule flows: `contacts-to-SFDC.xml` → `Contacts_to_SFDC`
- Spring Boot module: `spring-boot-migration/import-contacts-into-salesforce`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `file:inbound-endpoint path=src/main/resources/input pollingFrequency=10000 moveToDirectory=...output` (`Contacts_to_SFDC`) | `ContactsToSfdcFlow#contactsToSfdc`: Spring Integration `Files.inboundAdapter` + `Pollers.fixedDelay`, then move to `file.output-path` | No duplicate filter: a file with a name seen before is picked up again, as in Mule |
| `dw:transform-message` "Map CSV to Contacts" | `ContactsToSfdcFlow#toContacts` (Jackson CSV, header row) | Values stay strings; a missing column becomes `null` |
| `sfdc:create type=Contact` | `SalesforceClient#create("Contact", ...)` → `POST /composite/sobjects` |  |
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
| `sfdc.password` | `sfdc.password` | blank |
| `sfdc.securityToken` | `sfdc.securityToken` | blank |
| – | `sfdc.consumerKey` / `sfdc.consumerSecret` (new: Connected App) | blank |
| `sfdc:config url` (SOAP login endpoint) | `sfdc.login-url` | `https://login.salesforce.com` |
| – | `sfdc.api-version` | `61.0` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-import-sample-contacts-csv` | The sample contacts.csv dropped into the input folder is moved to the output folder and its two rows are created as Salesforce Contacts with the DataWeave field mapping (firstname->FirstName, surname->LastName, email->Email, phone->Phone; values stay strings, so the phone's leading zero is kept). | documented |
| `02-quoted-values-and-missing-column` | DataWeave's CSV reader honours quoted fields (embedded commas) and reads columns by header name regardless of order; a column absent from the file maps to null. | documented |

## Behavioural deltas

See [README.md](README.md#behavioural-deltas).
