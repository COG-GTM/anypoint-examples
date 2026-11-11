# Migration notes: `proxying-a-soap-api`

- Original Mule app: [`../../proxying-a-soap-api`](../../proxying-a-soap-api)
- Mule flows: `soap-api-proxy.xml` → `main`, sub-flow `copy-headers`
- Spring Boot module: `spring-boot-migration/proxying-a-soap-api`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener-config port="${http.port}"` | `server.port: ${http.port:8081}` | |
| `http:listener path="/"` (`main`) | `SoapApiProxyController#proxy` (`@PostMapping("/")`) | |
| `cxf:proxy-service payload="envelope" wsdlLocation=...` | `SoapEnvelopes.invalidReason` (SOAP 1.1 Envelope + Body check), `soap:Client` fault on failure | no remote WSDL (delta) |
| `flow-ref copy-headers` / `copy-properties *` / `remove-property ...` | `MuleHeaderFilter` | duplicated from proxying-a-rest-api (no cross-module deps) |
| `cxf:proxy-client payload="envelope"` | raw envelope bytes forwarded unchanged | |
| `http:request-config host="www.predic8.com" port="8080"` | `ShopServiceClientConfiguration#shopServiceClient` (`soap.target.base-url`) | WebClient / Reactor Netty |
| `http:request method="POST" path="shop/ShopService"` | `WebClient.post().uri(soap.target.path)` | |
| default success-status-code-validator → CXF fault | status >= 400 → `soap:Server` fault, HTTP 500 | |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| request config host/port | `soap.target.base-url` | `http://www.predic8.com:8080` |
| request path | `soap.target.path` | `/shop/ShopService` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-create-cart-envelope-proxied` | original `create` envelope forwarded unchanged (XMLUnit), `SOAPAction` forwarded, `<cartId>` reply returned (XMLUnit) | documented |
| `02-non-soap-payload-rejected-with-fault` | non-SOAP payload → SOAP fault 500, no downstream call | documented |
| `03-shop-service-fault-becomes-500` | ShopService 500 → SOAP fault 500 | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas): ShopService stubbed, no WSDL load / `?wsdl`, fault wording, h2c headers.
