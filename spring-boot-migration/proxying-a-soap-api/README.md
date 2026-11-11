# proxying-a-soap-api (Spring Boot)

Spring Boot 3 migration of the Mule 3.8 example [`proxying-a-soap-api`](../../proxying-a-soap-api): a SOAP proxy
in front of the predic8 ShopService. Every SOAP 1.1 envelope POSTed to `/` is checked and forwarded unchanged to
`http://www.predic8.com:8080/shop/ShopService`. The ShopService response envelope is returned to the caller.

Implementation: `SoapApiProxyController` (`@PostMapping("/")`) checks the envelope (`SoapEnvelopes`, the
`cxf:proxy-service payload="envelope"` step) and forwards the raw bytes with `WebClient` (`cxf:proxy-client` + `http:request`).
Headers are copied the same way as the Mule `copy-headers` sub-flow (`MuleHeaderFilter`).

## Run

```bash
cd spring-boot-migration
mvn -B -pl proxying-a-soap-api -am verify
java -jar proxying-a-soap-api/target/proxying-a-soap-api-1.0.0-SNAPSHOT.jar --http.port=8081
curl -H 'Content-Type: text/xml' --data @../proxying-a-soap-api/src/test/resources/message.xml http://localhost:8081/
```

| Property | Default | Meaning |
|---|---|---|
| `http.port` | `8081` | Listener port |
| `soap.target.base-url` | `http://www.predic8.com:8080` | Proxied SOAP service host (Mule `HTTP_Request_Configuration`) |
| `soap.target.path` | `/shop/ShopService` | Proxied SOAP service path |

## Parity tests

`ProxyingASoapApiParityTest` (`AbstractWireMockParityTest`) sends the original `message.xml` envelope. WireMock plays the
ShopService. The tests check the forwarded envelope (XMLUnit) and the `SOAPAction` header, the response envelope
(XMLUnit), rejection of non-SOAP payloads, and mapping of ShopService faults.

## Behavioural deltas

- **ShopService stubbed in tests.** `www.predic8.com:8080` is no longer online, so the original `ProxyingSoapApiIT`
  cannot run anymore. The parity tests use WireMock with a ShopService-shaped `createResponse`.
- **No WSDL-based validation or `?wsdl` publishing.** Mule's `cxf:proxy-service` loaded the remote WSDL at startup
  (the app would not start without it) and served it at `/?wsdl`. The Spring proxy does not depend on that WSDL. It
  checks only that the payload is a SOAP 1.1 `Envelope` with a `Body`, and anything else gets a `soap:Client` fault (500).
  `GET /?wsdl` is not served and returns 405.
- **Fault text.** Invalid envelopes and ShopService errors (HTTP >= 400) both produce a SOAP 1.1 fault with HTTP 500.
  The `faultstring` wording is not the same as CXF's. The fixtures assert only the status and the fault element.
- **HTTP/2 upgrade headers** (`Upgrade`, `HTTP2-Settings`) are not forwarded, the same as in `proxying-a-rest-api`.
