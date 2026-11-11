# Migration notes: `foreach-processing-and-choice-routing`

- Original Mule app: [`../../foreach-processing-and-choice-routing`](../../foreach-processing-and-choice-routing)
- Mule flows: `loanbroker-simple.xml` → `LoanBrokerFlow`, `lookupCustomerCreditProfile`, `lookupBanks`, `lookupLoanQuote`, `findLowestLoanQuote`, mock `CreditAgencyFlow`, `Bank1Flow`..`Bank5Flow`
- Spring Boot module: `spring-boot-migration/foreach-processing-and-choice-routing`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/*"` (port 11081) | `LoanBrokerController` (`/**`) |  |
| `choice` required fields | null checks → `Error: incomplete request` |  |
| `lookupCustomerCreditProfile` (cxf:jaxws-client + http:request) | `LoanBrokerClients#creditProfile` (header enricher) |  |
| `lookupBanks` `choice` | `BankRouting.banksFor` (header enricher) | `>= 10000 || <= 19999` is always true, so Bank 5 cannot be reached. Kept as in Mule. |
| `foreach` + `enricher target=#[quotes.add($)]` + `lookupLoanQuote` | Spring Integration `split()` over the banks → `handle(LoanBrokerClients#loanQuote)` → `aggregate()` |  |
| `findLowestLoanQuote` | `BankRouting.lowest` | Strict `<`, so the first quote wins a tie. |
| `object-to-string-transformer` (`LoanQuote.toString`) | `Model.LoanQuote#toString` |  |
| `catch-exception-strategy` → `Error processing loan request` | `try/catch` in the controller |  |
| mock `TheCreditAgencyService`, `TheBank1..5` | `MockServicesController` (`/mule/TheCreditAgencyService`, `/mule/TheBankN`) | Same random logic as `DefaultCreditAgency`/`Bank`. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| listener port 11081 | `server.port` (`${http.port:11081}`) | 11081 |
| credit agency / bank hosts | `loanbroker.services-base-url` | this app |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-large-loan-routes-to-bank1-and-bank2.json` | amount >= 20000 -> Bank 1 and Bank 2 are each asked once (foreach), the lowest rate wins. | `documented` |
| `02-boundary-20000-routes-to-bank1-and-bank2.json` | 20000 satisfies amount >= 20000. | `documented` |
| `03-medium-loan-routes-to-bank3-and-bank4.json` | 10000..19999 -> Bank 3 and Bank 4. | `munit` |
| `04-small-loan-still-routes-to-bank3-and-bank4.json` | The second condition 'amount >= 10000 \|\| amount <= 19999' is always true, so a 5000 loan also goes to Bank 3 and Bank 4; Bank 5 is never called. | `documented` |
| `05-equal-rates-keep-first-bank.json` | findLowestLoanQuote only replaces on a strictly lower rate, so the first bank in the routed order wins ties. | `documented` |
| `06-missing-term-is-incomplete.json` | Any missing name/ssn/amount/term -> 'Error: incomplete request' without calling any service. | `munit` |
| `07-no-parameters-is-incomplete.json` | The listener path is /* so any path is accepted; without parameters the request is incomplete. | `documented` |
| `08-non-numeric-amount-is-processing-error.json` | Integer.parseInt fails in 'Create customer request'; the catch-exception-strategy answers 'Error processing loan request'. | `documented` |
| `09-credit-agency-failure-is-processing-error.json` | A failing credit agency call is caught by the catch-exception-strategy; no bank is asked. | `documented` |

Other tests: `BankRoutingTableTest`, `ForeachProcessingAndChoiceRoutingParityTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

- The credit agency and banks are called with JSON over HTTP, not SOAP/CXF. These were internal mocks in the Mule app, and the public HTTP contract on port 11081 is the same.
- The mock services run on the app's own port under `/mule/...`, not on ports 18080 and 10080-50080.
- Mock credit scores and bank rates are still random, so parity fixtures stub them with WireMock to get fixed results.
