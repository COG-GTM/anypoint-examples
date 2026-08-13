# Legacy Modernization — Spring Boot 3 service

Java 21 / Spring Boot 3.3 migration of the Mule 3 [`legacy-modernization`](../../legacy-modernization)
example. It exposes the same SOAP endpoint on the same path and port, acknowledges the order to the
caller and, asynchronously, drops a CSV order file for the legacy fulfillment system.

See [MIGRATION.md](MIGRATION.md) for the flow-element → Spring-component mapping and the behaviour
deltas. The original Mule project is untouched.

## Run

```bash
mvn spring-boot:run
```

The service listens on `0.0.0.0:1080` and accepts `POST /OrderFulfillment`:

```bash
curl -s -X POST http://localhost:1080/OrderFulfillment \
  -H 'Content-Type: text/xml' \
  --data-binary @../../legacy-modernization/src/main/resources/FulfillmentShippingOrder_Input_Example.xml
```

The CSV order file appears in the drop folder configured by `legacy.fulfillment.output-directory`
(default `./output`) as `ShippingOrder-<dd-MM-yy_HH-mm-ss.SSS>.txt`.

## Test

```bash
mvn -q test
```

## Layout

| Package | Contents |
| --- | --- |
| `web` | `FulfillmentController` (the migrated HTTP listener) and the SOAP fault handler. |
| `soap` | Reader/writer replacing the CXF JAX-WS marshalling. |
| `service` | Order acknowledgement, the DataWeave-to-Java CSV transformer and the async dispatcher. |
| `legacy` | `LegacyFulfillmentGateway` port and its file-based adapter. |
| `model` | Immutable records mirroring the `org.ordermgmt` JAXB POJOs. |
