# Migration notes: `sap-data-retrieval`

- Original Mule app: [`../../sap-data-retrieval`](../../sap-data-retrieval)
- Mule flows: `sap-api.xml` → `sap-api-main`, `sap-api-console`, `get:/customers:sap-api-config`,
  `get:/salesOrders:sap-api-config`, `get:/materials:sap-api-config`
- Spring Boot module: `spring-boot-migration/sap-data-retrieval`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/api/*"` + `apikit:router` (`sap-api-main`) | `SapApiController` (`@RequestMapping("/api")`) | |
| `apikit:config raml="sap-api.raml"` (defaults `" "` for sales-order params) | `@RequestParam(defaultValue = " ")` | |
| `apikit:mapping-exception-strategy` | `ApiKitExceptionMapping` (`@RestControllerAdvice`) | |
| `sap:connector` (`sap.jco.*`) | `SapProperties` (`sap.jco.*`, `sap.gateway-base-url`) | Delta 1. |
| `dw:transform-message` → `BAPI_CUSTOMER_FIND` XML | `SapRequests.CustomerFind#of` + `SapGatewayClient#findCustomers` (`BapiXml#write`) | |
| `dw:transform-message` → `BAPI_MATERIAL_GETLIST` XML | `SapRequests.MaterialGetList#of` + `SapGatewayClient#getMaterials` | |
| `dw:transform-message` → `BAPI_SALESORDER_GETLIST` XML | `SapRequests.SalesOrderGetList` + `SapGatewayClient#getSalesOrders` | |
| `sap:outbound-endpoint type="function" xmlVersion="2" outputXml="true"` | `SapClient` interface / `SapGatewayClient` (WebClient POST `/functions/{BAPI}`, `application/xml`) | |
| `dw:transform-message` `*row map {($$):$}` (`application/json`) | `BapiXml#rows` → `List<SapRow>` (`@JsonAnyGetter`) | |
| `… when table != "" otherwise {message: "No data found."}` | `SapApiController#orNoData` | materials / salesOrders only. |
| `logger` | omitted | |
| `sap-api-console` | not migrated | Delta 4. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `sap.jco.ashost` | `sap.jco.ashost` | `localhost` |
| `sap.jco.sysnr` | `sap.jco.sysnr` | `00` |
| `sap.jco.client` | `sap.jco.client` (`sap-client` header) | `800` |
| `sap.jco.lang` | `sap.jco.lang` (`sap-language` header) | `EN` |
| `sap.jco.user` / `sap.jco.passwd` | `sap.jco.user` / `sap.jco.passwd` (Basic auth) | empty |
| (none; JCo RFC) | `sap.gateway-base-url` | `https://${sap.jco.ashost}:44300/sap/bapi` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-customers-by-name` | `BAPI_CUSTOMER_FIND` import/selection mapping; RESULT_TAB rows → `customers` | documented |
| `02-customers-without-name` | wildcard `*` selection | documented |
| `03-materials-by-name` | `BAPI_MATERIAL_GETLIST` mapping; MATNRLIST → `materials` | documented |
| `04-materials-multiple-rows` | all rows in order, empty fields `""`, entities decoded | documented |
| `05-materials-no-data` | empty table → `No data found.` | documented |
| `06-sales-orders-for-customer` | `BAPI_SALESORDER_GETLIST` mapping; SALES_ORDERS → `salesOrders` | documented |
| `07-sales-orders-defaults-no-data` | RAML `" "` defaults; empty table → `No data found.` | documented |
| `08-unknown-resource-404` | APIkit 404, no SAP call | documented |
| `09-method-not-allowed-405` | APIkit 405, no SAP call | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas).
