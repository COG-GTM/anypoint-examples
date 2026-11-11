# Migration notes: `get-customer-list-from-netsuite`

- Original Mule app: [`../../get-customer-list-from-netsuite`](../../get-customer-list-from-netsuite)
- Mule flows: `get-customer-list-from-netsuite.xml` → `get-customer-list-from-netsuiteFlow`
- Spring Boot module: `spring-boot-migration/get-customer-list-from-netsuite`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/customers"` | `CustomerListController#customers` (`@GetMapping("/customers")`) | |
| `expression-filter` (`lastName != null`) | early `ResponseEntity.ok().build()` when `lastName` is absent | Filtered Mule events give an empty 200; NetSuite isn't called. |
| `netsuite:config` (email/password/account/roleId) | `NetSuiteProperties` (`netsuite.*`) + `NLAuth` default header | |
| `netsuite:query` (DSQL `SELECT email,firstName,lastName FROM CUSTOMER WHERE lastName like '…%' ORDER BY lastName ASC`) | `NetSuiteQueryClient#query` (WebClient POST `/services/rest/query/v1/suiteql`, body `{"q": "<same DSQL>"}`) | Typed result `NetSuiteCustomer`. |
| `dw:transform-message` (`div/line/tr/td`, `otherwise` "No customers found") | `CustomerTableRenderer#rows` | |
| `object-to-string-transformer mimeType="text/html"` | `ResponseEntity…contentType(TEXT_HTML)` | |
| `parse-template location="customer/index.html"` + Groovy `tokenize('\n')[1..-1]` | `CustomerTableRenderer#render` (template copied to `templates/customer/index.html`) | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `netsuite.email` | `netsuite.email` | empty |
| `netsuite.password` | `netsuite.password` | empty |
| `netsuite.account` | `netsuite.account` | empty |
| `netsuite.roleId` | `netsuite.role-id` | empty |
| (connector-internal endpoint) | `netsuite.base-url` | `https://${netsuite.account}.suitetalk.api.netsuite.com` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-customers-by-last-name` | DSQL query + credentials sent to NetSuite; every record rendered as a row, in returned order | documented |
| `02-no-customers-found` | empty result renders the `No customers found` row | documented |
| `04-customers-across-result-pages` | all NetSuite result pages are fetched (`hasMore`/`offset`) and rendered in order | documented |
| `03-missing-last-name-is-filtered` | no `lastName` → empty 200, zero NetSuite calls | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas): SuiteQL REST transport instead of SuiteTalk SOAP, single-quote escaping,
whitespace between template tags not asserted (tag order is).
