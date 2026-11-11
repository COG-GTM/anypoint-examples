# web-service-consumer (Spring Boot)

Spring Boot 3 migration of the Mule 3.8 example [`web-service-consumer`](../../web-service-consumer): a consumer of the
T-shirt SOAP service (`tshirt.wsdl`, `http://tshirt-service.cloudhub.io`).

| Endpoint | Mule flow | Behaviour |
|---|---|---|
| `/orders` (JSON body) | `orderTshirt` | JSON → `ns0:OrderTshirt` + SOAP header `AuthenticationHeader/apiKey=abc12345` → `OrderTshirt` → response as JSON, 200 |
| `/inventory` | `listInventory` | `ListInventory` → `payload.ns0#ListInventoryResponse` as JSON, 200 |

Implementation: `TshirtController` (Spring MVC), `TshirtServiceClient` (Spring-WS `WebServiceTemplate`, SOAP 1.1,
SOAPAction from the WSDL), and `DataWeaveJson`, which reproduces the two DataWeave 1.0 transformations. Repeated XML
elements are kept as repeated JSON keys, as DataWeave 1.0 does.

## Run

```bash
cd spring-boot-migration
mvn -B -pl web-service-consumer -am verify
java -jar web-service-consumer/target/web-service-consumer-1.0.0-SNAPSHOT.jar --http.port=8081
curl -H 'Content-Type: application/json' --data @../web-service-consumer/src/test/resources/message.json http://localhost:8081/orders
curl -X POST http://localhost:8081/inventory
```

| Property | Default | Meaning |
|---|---|---|
| `http.port` | `8081` | Listener port |
| `tshirt.service.address` | `http://tshirt-service.cloudhub.io` | Mule `ws:consumer-config serviceAddress` |
| `tshirt.service.api-key` | `abc12345` | value of the `AuthenticationHeader/apiKey` SOAP header (hard-coded in the Mule DataWeave) |

## Parity tests

`WebServiceConsumerParityTest` (`AbstractWireMockParityTest`) sends the original `message.json`. WireMock plays the
T-shirt service. Each fixture checks the outgoing SOAP envelope (XMLUnit, including the `AuthenticationHeader`) and the
`SOAPAction`, then the JSON returned to the caller. `/inventory` is compared as text because its body has duplicate
`inventory` keys, which a JSON parser would merge. `ApplicationStartupTest` checks that the app starts with its
production defaults.

## Behavioural deltas

- **T-shirt service stubbed in tests.** The original IT called the CloudHub service. The parity tests use WireMock with
  the original `sample_data` responses wrapped in SOAP envelopes.
- **Error bodies.** A SOAP fault or transport error returns HTTP 500 in both apps. The body is Spring Boot's default
  error JSON, not Mule's exception text, which is not documented. The fixture asserts only the status.
- **No WSDL operation validation at startup.** Mule's `ws:consumer` read `tshirt.wsdl` at startup. Here the operations
  and SOAPActions are constants taken from that WSDL, which is kept in `src/main/resources` for reference.
