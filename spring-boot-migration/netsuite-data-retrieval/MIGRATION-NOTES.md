# Migration notes: `netsuite-data-retrieval`

- Original Mule app: [`../../netsuite-data-retrieval`](../../netsuite-data-retrieval)
- Mule flows: `netsuite-api.xml` → `netsuite-api-main`, `netsuite-api-console`, `get:/customers:netsuite-api-config`,
  `get:/items:netsuite-api-config`, `get:/oportunities:netsuite-api-config`
- Spring Boot module: `spring-boot-migration/netsuite-data-retrieval`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/api/*"` + `apikit:router` (`netsuite-api-main`) | `NetSuiteApiController` (`@RequestMapping("/api")`) | |
| `apikit:config raml="netsuite-api.raml"` query parameter defaults/validation | `@RequestParam(defaultValue=…)` + `QUANTITY_OPERATORS` / `Long.parseLong` → `ApiKitBadRequestException` | |
| `apikit:flow-mapping resource="/opportunities" flow-ref="get:/oportunities…"` | `NetSuiteApiController#opportunities` (`@GetMapping("/opportunities")`) | |
| `apikit:mapping-exception-strategy` (404/405/415/406/400) | `ApiKitExceptionMapping` (`@RestControllerAdvice`) | 406 not mapped (delta 3). |
| `netsuite:config-login-authentication` (`nets.*`) | `NetSuiteProperties` + `NLAuth` default header on the `WebClient` | |
| `dw:transform-message` → `CustomerSearch` (`companyName STARTS_WITH`, conditional) | `NetSuiteSearch.CustomerSearch#companyNameStartsWith` (`@JsonInclude(NON_NULL)`) | |
| `dw:transform-message` → `ItemSupplyPlanSearchBasic` (`quantity as :number`) | `NetSuiteSearch.ItemSupplyPlanSearchBasic` / `SearchLongField` | |
| `dw:transform-message` → `OpportunitySearch` (`title STARTS_WITH`, conditional) | `NetSuiteSearch.OpportunitySearch#titleStartsWith` | |
| `netsuite:search searchRecord="CUSTOMER" / "ITEM_SUPPLY_PLAN_BASIC" / "OPPORTUNITY"` | `NetSuiteSearchClient#searchCustomers` / `#searchItemSupplyPlans` / `#searchOpportunities` (WebClient POST `/search/{searchRecord}`) | Typed criteria in, `List<NetSuiteRecord>` out. |
| `dw:transform-message` `payload map $` (`application/json`) | Jackson serialisation of `List<NetSuiteRecord>` (`produces = application/json`) | |
| `logger` | omitted | |
| `netsuite-api-console` (`apikit:console`) | not migrated | Delta 2. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| listener port `8081` (hard-coded) | `server.port` (`${http.port:8081}`) | 8081 |
| `nets.email` | `nets.email` | empty |
| `nets.password` | `nets.password` | empty |
| `nets.account` | `nets.account` | empty |
| `nets.roleId` | `nets.role-id` | empty |
| `nets.applicationId` | `nets.application-id` | empty |
| (connector-internal endpoint) | `nets.base-url` | `https://${nets.account}.suitetalk.api.netsuite.com` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-customers-by-name` | `companyName STARTS_WITH` criteria + credentials; records returned unchanged | documented |
| `02-customers-without-name` | conditional element omitted → `{"basic":{}}` | documented |
| `03-items-with-raml-defaults` | RAML defaults `quantity=0`, `operator=GREATER_THAN`; numeric `searchValue` | documented |
| `04-items-with-quantity-and-operator` | explicit operator/quantity mapping | documented |
| `05-opportunities-by-title` | `/opportunities` routed to the `oportunities` flow, `title STARTS_WITH` | documented |
| `06-opportunities-without-title` | empty `basic` criteria | documented |
| `07-unknown-resource-404` | APIkit 404 body, no NetSuite call | documented |
| `08-method-not-allowed-405` | APIkit 405 body, no NetSuite call | documented |
| `09-items-invalid-operator-400` | RAML enum validation → 400 | documented |
| `10-items-non-integer-quantity-400` | RAML integer validation → 400 | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas).
