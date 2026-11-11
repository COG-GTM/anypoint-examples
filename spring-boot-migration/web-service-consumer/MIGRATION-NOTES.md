# Migration notes: `web-service-consumer`

- Original Mule app: [`../../web-service-consumer`](../../web-service-consumer)
- Mule flows: `tshirt-service-consumer.xml` → `orderTshirt`, `listInventory`
- Spring Boot module: `spring-boot-migration/web-service-consumer`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener-config port="${http.port}"` | `server.port: ${http.port:8081}` | |
| `http:listener path="/orders"` + `response-builder statusCode="200"` (`orderTshirt`) | `TshirtController#orderTshirt` (`@RequestMapping("/orders")`) | any method, like the Mule listener |
| `dw:set-payload` `ns0#OrderTshirt: payload` (JSON → XML) | `DataWeaveJson.toXmlElement` | field order preserved |
| `dw:set-property soap.header` `ns0#AuthenticationHeader: {apiKey: "abc12345"}` | `TshirtServiceClient#orderTshirt` adds the header element (`tshirt.service.api-key`) | |
| `ws:consumer-config wsdlLocation="tshirt.wsdl" serviceAddress=...` | `TshirtServiceClient` (`WebServiceTemplate`, `SaajSoapMessageFactory`, `tshirt.service.address`) | SOAP 1.1 |
| `ws:consumer operation="OrderTshirt"` | `sendSourceAndReceiveToResult` with SOAPAction `http://mulesoft.org/tshirt-service/order-tshirt` | returns the Body's first child |
| `ws:consumer operation="ListInventory"` (no input) | generated `<ns0:ListInventory/>`, SOAPAction `.../list-inventory`, empty `soap:Header` removed | |
| `dw:set-payload` `%output application/json payload` | `DataWeaveJson.document` | |
| `http:listener path="/inventory"` (`listInventory`) | `TshirtController#listInventory` (`@RequestMapping("/inventory")`) | |
| `dw:set-payload` `payload.ns0#ListInventoryResponse` | `DataWeaveJson.content` | repeated keys kept |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `serviceAddress` | `tshirt.service.address` | `http://tshirt-service.cloudhub.io` |
| apiKey literal in DataWeave | `tshirt.service.api-key` | `abc12345` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-order-tshirt` | outbound `OrderTshirt` envelope + `AuthenticationHeader` + SOAPAction (XMLUnit); `{"OrderTshirtResponse":{"orderId":"1"}}` | documented |
| `02-list-inventory` | outbound `ListInventory` envelope + SOAPAction; DataWeave JSON with repeated `inventory` keys (text) | documented |
| `03-soap-fault-fails-with-500` | SOAP fault → 500 | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas): service stubbed in tests, error body format, no WSDL load at startup.
