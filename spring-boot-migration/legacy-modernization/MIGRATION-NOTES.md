# Migration notes: `legacy-modernization`

- Original Mule app: [`../../legacy-modernization`](../../legacy-modernization)
- Mule flows: `FufillmentWebService.xml` → `Fulfillment_LegacySystemModernization`
- Spring Boot module: `spring-boot-migration/legacy-modernization`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener port=1080 allowedMethods="POST" path="OrderFulfillment"` | Spring WS `MessageDispatcherServlet` at `spring.webservices.path=/OrderFulfillment`, `server.port=1080` | GET → 405 |
| `cxf:jaxws-service serviceClass="org.ordermgmt.IFulfillment"` | Spring WS `@Endpoint FulfillmentEndpoint` + `@PayloadRoot(namespace="http://ordermgmt.org/", localPart="putShippingOrder")`, JAXB `PutShippingOrder` / `PutShippingOrderResponse` | Document/literal wrapped, unqualified child elements as with CXF |
| `component class="org.ordermgmt.FulfillmentImpl"` | `FulfillmentEndpoint#putShippingOrder` | Builds the same confirmation |
| CXF envelope without `soap:Header` | `SoapEnvelopeConfig` interceptor removes the empty SAAJ header |  |
| `async` scope | `@Async LegacyFulfillmentFileWriter#write` (`@EnableAsync`) |  |
| `dw:transform-message` `%output application/csv header=true` | `LegacyFulfillmentCsv#render` | Same columns, order and values |
| `file:outbound-endpoint path="src/main/resources/Output" outputPattern="ShippingOrder-#[server.dateTime.format('dd-MM-yy_HH-mm-ss.SSS')].txt"` | `java.nio.file.Files#writeString` into `legacy.output.path` | Same file name pattern |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` (hard-coded 1080 in the listener config) | `server.port` (`${http.port}`) | 1080 |
| file endpoint `path` | `legacy.output.path` | `src/main/resources/Output` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-put-shipping-order.json` | putShippingOrder echoes the order in a ShippingOrderConfirmation with orderReceivedStatus=true, and the async scope writes one CSV file with a header and one record per order item. | `documented` |
| `02-single-item-order.json` | An order with one item yields a confirmation echoing it and a CSV file with exactly one record. | `documented` |
| `03-get-not-allowed.json` | The listener only allows POST: a GET is rejected with 405 and nothing is written. | `documented` |

## Behavioural deltas

- CXF is replaced by Spring WS. No WSDL is published at `?wsdl`, and SOAP faults for malformed requests use Spring WS's fault format and wording instead of CXF's.
- The CSV layout (comma separator, LF line endings, a trailing newline, quoting only when a value contains a separator, quote or newline) follows DataWeave 1.0 CSV writer defaults. It is derived from the documentation and has not been captured from a running Mule.
