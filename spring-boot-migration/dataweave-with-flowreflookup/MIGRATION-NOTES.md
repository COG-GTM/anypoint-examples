# Migration notes: `dataweave-with-flowreflookup`

- Original Mule app: [`../../dataweave-with-flowreflookup`](../../dataweave-with-flowreflookup)
- Mule flows: `dataweave-with-flowreflookup.xml` → `createNewSalesforceAccount`, `lookupSalesRegion`
- Spring Boot module: `spring-boot-migration/dataweave-with-flowreflookup`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `file:inbound-endpoint` (input → output move) | Spring Integration file inbound adapter + move in `CreateNewSalesforceAccountFlowConfig` |  |
| DataWeave with `lookup("lookupSalesRegion", ...)` | `AccountMapper` + `SalesRegionLookup` | `flow-ref` lookup → Java method. |
| Groovy state → region table | `SalesRegionLookup` | Unknown state → `UNKNOWN`. |
| `sfdc:create type=Account` | `SalesforceClient` → `RestSalesforceClient` (composite sObjects REST) | Typed client seam; WireMock in tests. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `sfdc.*` | `salesforce.base-url`, `salesforce.access-token`, `salesforce.api-version` | `SALESFORCE_*` env vars, v59.0 |
| `input.dir` / `output.dir` / `polling.frequency` | same names | `src/main/resources/input` / `output` / 10000 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-sample-companies.json` | The bundled companies.csv: BillingState is upper-cased and the region comes from the lookup flow (fl -> South East, CA -> West Coast); both rows go to one Salesforce create call. | `munit` |
| `02-region-table.json` | Every region branch of the Groovy switch, plus states not listed (UNKNOWN) and an empty state (UNKNOWN, BillingState empty). The extra has_given_contact_permission column is not mapped. | `documented` |
| `03-salesforce-error-still-moves-file.json` | A failed Salesforce create is not retried: the file was already moved by moveToDirectory when it was read, and exactly one create call is made. | `documented` |

Other tests: `DataweaveWithFlowreflookupFileParityTest`, `SalesRegionLookupTableTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

- The Mule Salesforce connector (username/password/security token) became REST calls to `/services/data/{version}/composite/sobjects` with an OAuth bearer token (`salesforce.access-token`). Tests run it against WireMock.
- The app has no HTTP endpoint, same as the Mule app. Parity fixtures use `method: FILE` and drop the CSV into the input folder.
