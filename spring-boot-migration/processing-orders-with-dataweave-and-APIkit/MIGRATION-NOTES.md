# Migration notes: `processing-orders-with-dataweave-and-APIkit`

- Original Mule app: [`../../processing-orders-with-dataweave-and-APIkit`](../../processing-orders-with-dataweave-and-APIkit)
- Mule flows: `books.xml` → `OrderFlow`; `currency.xml` → `currency-main`, `get:/currencies:currency-config`
- Spring Boot module: `spring-boot-migration/processing-orders-with-dataweave-and-APIkit`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `file:inbound-endpoint path="src/main/resources/input"` (`OrderFlow`) | Spring Integration `Files.inboundAdapter(orders.input-dir)` + poller (`OrderFlow#orderIntegrationFlow`) | The consumed file is deleted (Mule autoDelete) |
| `enricher target=flowVars.currencies` + `http:request GET /currencies` (`basePath=/api/`, `localhost:${http.port}`) | `RestClient` GET `<currency base>/api/currencies` (`OrderFlow#process`) | Real HTTP loopback to this app |
| `dw:set-payload` (orders, filter year > 2004, price × ratio, `distinctBy` authors) | `OrderMapper#toOrders` (plain Java + `BigDecimal`, Jackson) |  |
| `dw:set-variable report` (`%output application/csv`: sizeOf / sum / avg) | `OrderMapper#toReportCsv` |  |
| `file:outbound-endpoint outputPattern="orders.json"` / `"report.csv"` | `Files.writeString(orders.output-dir/...)` |  |
| XML input (`orders.xsd`) | `OrdersXmlReader` (DOM, secure processing, no DOCTYPE) |  |
| `http:listener path="/api/*"` + `apikit:router raml=currency.raml` + `parse-template currency.json` | `CurrencyController#currencies` (`@GetMapping("/api/currencies")`) |  |
| `apikit:mapping-exception-strategy currency-apiKitGlobalExceptionMapping` | `CurrencyApiExceptionMapping` (`@RestControllerAdvice`) |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| (hard-coded) `src/main/resources/input` | `orders.input-dir` | `src/main/resources/input` |
| (hard-coded) `src/main/resources/output` | `orders.output-dir` | `src/main/resources/output` |
| `http.port` (request config) | `orders.currency-base-url` | blank = `http://localhost:<local.server.port>` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| 01-get-currencies | Currency API body | documented |
| 02-unknown-resource-404 | APIkit 404 mapping | documented |
| 03-post-currencies-405 | APIkit 405 mapping | documented |
| `OrderFlowParityTest` | File in → Mule-expected `orders.json` + `report.csv` out, input consumed | munit |
| `OrderMapperTest` | DataWeave semantics | documented |

## Behavioural deltas

- **`orders.json` whitespace.** Spring writes Jackson pretty-printed JSON, so the whitespace differs from DataWeave's output. Keys, order and number text (`27.60`, `262.7124`, ...) are the same. The Mule IT and the parity test compare with whitespace stripped.
- **Hidden files are ignored.** The inbound adapter skips dot-files. This keeps the `.gitkeep` in `src/main/resources/input` from being treated as an order file. Mule would try to process any file it finds.
- **Failure handling.** If a file can't be parsed or the currency call fails, Spring logs the error and leaves the file in place. Mule's default strategy also logs, and does not write output.
- **Paths outside `/api`.** Requests there get Spring's 404 handling instead of Mule's "no listener" response. The APIkit console is replaced by springdoc (`/v3/api-docs`, `/swagger-ui.html`).
