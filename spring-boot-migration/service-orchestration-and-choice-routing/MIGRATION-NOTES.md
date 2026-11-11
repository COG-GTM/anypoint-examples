# Migration notes: `service-orchestration-and-choice-routing`

- Original Mule app: [`../../service-orchestration-and-choice-routing`](../../service-orchestration-and-choice-routing)
- Mule flows: `fulfillment.xml` → `orderService`, `samsungOrder`, `samsungWebServiceClient`, `inhouseOrder`, `auditService`, `priceService`, `samsungService`, `databaseInitialisation`; `mule-config.xml` → `orderRequest`, `orderProxy`, `manufacturers`
- Spring Boot module: `spring-boot-migration/service-orchestration-and-choice-routing`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="orders"` + `cxf:jaxws-service IProcessOrder` | `OrderController#processOrder` + `OrderSoap` (doc/literal SOAP envelope) |  |
| `foreach` + `enricher` + `choice manufacturer == 'Samsung'` | Spring Integration `split()` → `route()` → `aggregate()` in `OrderOrchestrationFlowConfig` |  |
| `vm://samsungOrder` (DataWeave → cxf client → status filter → receipt; catch → REJECTED) | `OrderFulfillment#samsungOrder` + `SamsungClient` |  |
| `jms inhouseOrder` (XA) with `ee:cache` price lookup + `db:insert` | `OrderFulfillment#inhouseOrder` (`@Transactional`) + `PriceClient` (`@Cacheable("prices")`) + `OrderRepository` | The cache key is the product id. |
| `vm://audit` one-way → `auditService` db:insert | `wireTap` → executor channel `audit` → `auditServiceFlow` |  |
| `databaseInitialisation` (`/populate`) | `OrderController#populate` |  |
| Derby XA datasource | embedded Derby via `spring.datasource` |  |
| `priceService` (Jersey `ProductPrice`), `samsungService` (`SamsungServiceImpl`) | `MockServicesController` |  |
| AJAX channels `/orders/request`, `/orders/soap`, `/orders/manufacturers` | HTTP endpoints with the same paths in `OrderController` |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| listener ports 1080/8091/9090/9999 | `server.port` (`${http.port:1080}`) | 1080 |
| Samsung service host | `samsung.base-url` | this app |
| price service host | `price.base-url` | this app |
| Derby URL | `spring.datasource.url` | `jdbc:derby:memory:muleEmbeddedDB` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-populate-creates-tables.json` | First GET /populate creates the orders and order_audits tables. | `munit` |
| `02-populate-again-reports-existing-tables.json` | The DDL fails the second time; the catch-exception-strategy answers 'table already populated'. | `documented` |
| `03-samsung-order.json` | The MUnit/IT message.xml order (one Samsung item, quantity 1) is routed to samsungOrder; the receipt {id 1, ACCEPTED, 2550.0} from the Samsung service enriches the item. | `documented` |
| `04-mixed-order-routes-by-manufacturer.json` | Choice per item: Samsung -> samsungOrder (2550 * 5 = 12750.0); Philips -> inhouseOrder (price 159 from the price service * 10 = 1590.0, no receipt id). | `documented` |
| `05-inhouse-price-is-cached-by-product-id.json` | ee:cache keyed by #[payload.productId]: product 23 was priced in the previous order, so no price call is made; a lower-case 'samsung' is not the Samsung branch. | `documented` |
| `06-samsung-failure-is-rejected.json` | A non-200 Samsung reply fails the http.status=200 filter; the catch-exception-strategy returns a REJECTED receipt with totalPrice 0. | `documented` |
| `07-ajax-order-request.json` | The /orders/request channel: sample-request.json goes through orderService and comes back in the sample-response.json shape. Receipts follow the flows (Galaxy 30 x 2550, product 23 cached at 159 x 10); the sample's copied Samsung receipt on the Philips item is not reproducible. | `documented` |
| `08-ajax-manufacturers.json` | The /orders/manufacturers channel returns a fixed list. | `documented` |
| `09-ajax-soap-proxy.json` | The /orders/soap channel forwards the SOAP envelope to orderService and returns its reply. | `documented` |
| `10-orders-get-not-allowed.json` | The orderService listener only allows POST. | `documented` |

Other tests: `OrderRoutingTableTest`, `OrderSideEffectsTest`, `ServiceOrchestrationAndChoiceRoutingParityTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

- All of Mule's listeners (1080, 8091, 9090, 9999) share one port, by default 1080.
- The AJAX (CometD) channels on port 8090 are now plain HTTP endpoints, and the bundled `docroot` demo page is not ported.
- JMS/XA transactions and ActiveMQ became one local `@Transactional` call. JMS redelivery (`maxRedeliveryAttempts=3`) is not reproduced; an in-house failure returns a SOAP fault (500).
- Derby runs in memory, not on disk.
- Mule's `priceService` listener allows only POST, even though `inhouseOrder` calls it with GET. The bundled price service here answers GET so the in-house branch works.
- `totalValue` is added up as a number. MEL coerced the Samsung `price` string.
- The Mule IT's `reply.xml` is empty, and `sample-response.json` copies the Samsung receipt onto the Philips item. Fixtures follow the flow logic and the `summary.xml` shape instead.
