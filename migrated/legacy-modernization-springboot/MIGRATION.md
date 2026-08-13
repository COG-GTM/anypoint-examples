# Migration: `legacy-modernization` (Mule 3.8) → Spring Boot 3.3 / Java 21

This is the reference slice of a MuleSoft → Spring Boot migration programme: one complete Mule
application, migrated behaviour-for-behaviour, with the original flow XML left untouched.

## What the Mule application does

`legacy-modernization/src/main/app/FufillmentWebService.xml` contains a single flow,
`Fulfillment_LegacySystemModernization`:

1. `http:listener` — `POST http://0.0.0.0:1080/OrderFulfillment`.
2. `cxf:jaxws-service` — exposes the JAX-WS interface `org.ordermgmt.IFulfillment`
   (operation `putShippingOrder`, namespace `http://ordermgmt.org/`) and unmarshals the SOAP body
   into `shippingId`, `billingAddress`, `shippingAddress` and `order`.
3. `component org.ordermgmt.FulfillmentImpl` — builds a `ShippingOrder`, wraps it in a
   `ShippingOrderConfirmation` with `orderReceivedStatus = true` and returns it to the caller.
4. `async` scope — off the request thread:
   - `dw:transform-message` maps the confirmation to CSV (one row per order item),
   - `byte-array-to-string-transformer`,
   - `file:outbound-endpoint` writes
     `ShippingOrder-#[server.dateTime.format('dd-MM-yy_HH-mm-ss.SSS')].txt` into
     `src/main/resources/Output` — the drop folder of the legacy, file-only fulfillment system.

There is no explicit exception strategy, so Mule's default strategy applies and CXF renders
failures as SOAP faults.

## Flow element → Spring component mapping

| Mule element | Spring component | Notes |
| --- | --- | --- |
| `http:listener-config` (`0.0.0.0:1080`) | `server.address` / `server.port` in `application.yml` | Same host and port, so existing callers are unchanged. |
| `http:listener` `path="OrderFulfillment"` `allowedMethods="POST"` | `FulfillmentController#putShippingOrder` (`@PostMapping("/OrderFulfillment")`) | Other methods produce 405, as with `allowedMethods`. |
| `cxf:jaxws-service` (request unmarshalling) | `ShippingOrderSoapReader` | JDK DOM parsing of the `putShippingOrder` body; no XML-binding dependency added. |
| `cxf:jaxws-service` (response marshalling) | `ShippingOrderSoapWriter` | Byte-for-byte reproduction of the CXF reply captured in `legacy-modernization/src/test/resources/reply.xml` (asserted by `ShippingOrderSoapWriterTest`). |
| `org.ordermgmt.IFulfillment` / `FulfillmentImpl` | `FulfillmentService` | Same acknowledge-then-forward semantics. |
| `Address`, `Order`, `OrderItem`, `ShippingOrder`, `ShippingOrderConfirmation` (JAXB POJOs) | `org.ordermgmt.fulfillment.model.*` Java records | Immutable; JAXB annotations dropped because binding is explicit. |
| `async` scope | `LegacyFulfillmentDispatcher` (`@Async("legacyFulfillmentExecutor")`) + `AsyncConfiguration` | Bounded `ThreadPoolTaskExecutor` replaces the Mule async processing strategy. |
| `dw:transform-message` "Map Fullfilment order to CSV" | `ShippingOrderCsvTransformer` | The DataWeave 1.0 script is quoted verbatim in the class Javadoc. |
| `byte-array-to-string-transformer` | (none needed) | The transformer already produces a `String`. |
| `file:outbound-endpoint` "LegacyFulfillment" | `LegacyFulfillmentGateway` (port) + `FileLegacyFulfillmentGateway` (adapter) | Same `ShippingOrder-dd-MM-yy_HH-mm-ss.SSS.txt` naming; directory configurable via `legacy.fulfillment.output-directory`. |
| Mule default exception strategy + CXF fault rendering | `SoapFaultExceptionHandler` (`@RestControllerAdvice`) | Returns `soap:Fault` bodies rather than Spring's JSON error body. |
| `LegacyModernizationIT` (MUnit-era functional test) | `FulfillmentFlowIntegrationTest` | Posts the same `message.xml` fixture and asserts the CSV lands in the drop folder. |

## Behaviour differences we could not preserve exactly

1. **No WSDL is published.** The Mule CXF endpoint served `?wsdl` and validated against the
   JAX-WS contract. The Spring service parses the envelope directly, so `GET /OrderFulfillment?wsdl`
   returns 405. If consumers generate stubs from the live WSDL, either keep a checked-in WSDL or
   add `spring-boot-starter-web-services`; we deliberately avoided that dependency for this slice.
2. **Fault HTTP status codes.** CXF maps unmarshalling failures to a `soap:Client` fault and server
   errors to a `soap:Server` fault; the migrated service returns 400 and 500 respectively. We could
   not run Mule 3.8 to confirm the exact status CXF emitted for client faults on this runtime
   (believed to be 400 in CXF 3.x), so treat the 400 as the one intentional deviation to confirm
   against the production gateway before cut-over.
3. **Fault body detail.** CXF faults carried a stack-trace-derived `faultstring`; ours carries a
   fixed message (`Order could not be processed`) and logs the exception, to avoid leaking internals.
4. **CSV quoting.** DataWeave 1.0 `application/csv` quotes only when required; the transformer does
   the same (separator, quote or newline in a value), but exotic DataWeave quoting options
   (`quoteValues`, custom separators) were not configured in the original and are not implemented.
5. **Null street concatenation.** `line1 ++ ' ' ++ line2` yields `null` in DataWeave when either
   line is absent, which renders as an empty CSV cell. The Java version reproduces the empty cell
   rather than emitting `"null"`.
6. **Async failure handling.** Mule's `async` scope silently drops the branch's exceptions; the
   dispatcher does the same, but logs at ERROR. No retry/DLQ was added — that is an explicit
   decision for the target platform, not a migration artefact.
7. **File-write semantics.** The Mule file connector's `responseTimeout="10000"` has no equivalent
   for a local write and was dropped. Files are written non-atomically, exactly as before.
8. **Output location.** Writing into `src/main/resources/Output` is a Studio-only convenience; the
   Spring service defaults to `./output` and expects a real drop folder to be configured per
   environment.

## What a team would do for the remaining apps in this repo

- **Classify first.** The ~60 examples fall into a handful of shapes: HTTP/REST façades
  (`exposing-a-restful-resource-using-the-HTTP-connector`, `rest-api-with-apikit`), SaaS connector
  integrations (Salesforce, NetSuite, Workday, SAP), messaging (`sending-json-data-to-a-jms-queue`,
  `websphere-mq`), file/batch (`upload-to-ftp-after-converting-json-to-xml`), and pure routing
  demos (`content-based-routing`, `scatter-gather-flow-control`). Each shape needs its target
  pattern agreed **once**, then applied mechanically.
- **Reusable target patterns** (already exercised here): listener → `@RestController`;
  DataWeave → a tested transformer class with the script quoted in Javadoc; connector → port
  interface + adapter (`RestClient` for HTTP, `JmsTemplate` for JMS/MQ, `JdbcClient` for DB,
  Spring Integration for FTP/file polling); `choice` → strategy or `switch`; `scatter-gather` →
  `CompletableFuture.allOf`; exception strategy → `@RestControllerAdvice`.
- **Contract-first for APIkit apps.** `rest-api-with-apikit` and friends already carry RAML;
  convert to OpenAPI 3 and generate controller interfaces so the contract remains the source of truth.
- **Parity harness.** Replay recorded requests against Mule and Spring and diff responses plus the
  side effects (file drops, queue messages). The response fixture reuse in this PR
  (`reply.xml` asserted byte-for-byte) is the pattern to scale.
- **Sequencing.** Migrate leaf integrations first, then anything fronted by API gateways, keeping
  paths and ports identical so routing can be flipped per-endpoint. One app per PR, roughly one
  session per app for the simple shapes and two or three for the connector-heavy ones.
