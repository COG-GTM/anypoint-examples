# soap-webservice-security (Spring Boot)

Spring Boot 3 migration of the Mule 3.8 example [`soap-webservice-security`](../../soap-webservice-security): one
JAX-WS `Greeter` (`greet(name)` → `Hello <name>`) published six times, each with a different WS-Security setup, plus a
client flow that calls them.

| Path (port 63081) | Mule flow | WS-Security validation (`cxf:ws-security` → WSS4J) |
|---|---|---|
| `/services/unsecure` | `UnsecureServiceFlow` | none |
| `/services/username` | `UsernameTokenServiceFlow` | `UsernameToken Timestamp` (`PasswordCallback`: joe/secret, stan/elephant) |
| `/services/signed` | `UsernameTokenSignedServiceFlow` | `UsernameToken Signature Timestamp` (`keystore.jks`) |
| `/services/encrypted` | `UsernameTokenEncryptedServiceFlow` | `UsernameToken Timestamp Encrypt` (`keystore.jks`) |
| `/services/saml` | `SamlTokenServiceFlow` | `SAMLTokenUnsigned Timestamp` + `SamlCustomValidator` |
| `/services/signedsaml` | `SignedSamlTokenServiceFlow` | `SAMLTokenUnsigned Signature` + `SamlCustomValidator` |
| `GET /client?clientType=..&name=..` (port 63080) | `SecurityClients` | client side: secures the request for the chosen service and returns the greeting as `text/plain` |

Implementation: a Spring-WS `MessageDispatcherServlet` mapped to the six paths and one `@Endpoint` (`GreeterEndpoint`).
Each secured flow is a Spring-WS `Wss4jSecurityInterceptor` (WSS4J 2.4) with the flow's actions, applied only to its own
path (`PathScopedInterceptor`). The client flow (`SecurityClientController`) uses `WebServiceTemplate` with
client-side `Wss4jSecurityInterceptor`s built from the `service-clients.xml` settings (`SecurityClients`).

## Run

```bash
cd spring-boot-migration
mvn -B -pl soap-webservice-security -am verify
java -jar soap-webservice-security/target/soap-webservice-security-1.0.0-SNAPSHOT.jar
curl 'http://localhost:63080/client?clientType=usernameTokenSigned&name=Mule'   # Hello Mule
```

| Property | Default | Meaning |
|---|---|---|
| `http.port` | `63081` | Service listener (Mule `HTTP_Listener_Configuration1`) |
| `security.client.port` | `63080` | Second connector for the client flow (Mule `HTTP_Listener_Configuration`); `-1` disables it |
| `security.services.base-url` | blank (= `http://localhost:<server port>/services`) | Where the client flow sends requests (Mule `HTTP_Request_Configuration`) |
| `security.keystore.file` / `.password` / `.alias` | `keystore.jks` / `keyStorePassword` / `joe` | Mule `wssecurity.properties` (Merlin) |

The keystore is the original Mule `keystore.jks`, copied unchanged.

## Parity tests

`SoapWebserviceSecurityParityTest` (`AbstractParityTest`, 16 fixtures) covers every case of the Mule
`SoapWebserviceSecurityIT`, including the failures (wrong password, unsigned request to the signed service, SAML
assertion from the wrong issuer), plus the `/client` flow for every `clientType`. WS-Security headers contain
timestamps, nonces and signatures, so they cannot be stored in a fixture. Instead, a fixture names the Mule client config in an
`X-Parity-WSS` request header, and the test secures the fixture's plain envelope with that WSS4J config right before
sending it (the header is not sent). Responses are compared with XMLUnit. Fault codes and strings are matched with
`~regex` text nodes on the phrases the Mule IT asserts (`security token`, `<wsse:Security> header`).
`ApplicationStartupTest` starts the app with production defaults.

## Behavioural deltas

- **WSS4J 2.4 instead of WSS4J 1.6 (CXF 2.5 in Mule 3.8).** Fault codes (`wsse:FailedAuthentication`,
  `wsse:InvalidSecurity`) and the phrases the Mule IT checks stay the same. The full fault strings and HTTP 500 status
  come from Spring-WS. Like CXF, any validation failure other than failed authentication is reported as
  `InvalidSecurity`.
- **Unsigned sender-vouches SAML.** WSS4J 2 rejects an unsigned sender-vouches assertion by default. Mule accepted it
  (`testSamlToken`), so the two SAML flows switch off `validateSamlSubjectConfirmation`.
  `SamlCustomValidator` still enforces issuer `self`, SAML 2.0, sender-vouches and subject `AllowGreetingServices`.
- **Encrypted bodies.** Spring-WS normally routes by payload root, which is only readable after decryption. A path-based
  fallback mapping sends any request on a service path to the greeter, as Mule routed by listener path.
- **Historical crypto kept as is.** `keystore.jks` holds a 1024-bit RSA key with an `MD5withRSA` self-signed
  certificate that expires in 2029. It is used unchanged, so it is no stronger than the Mule demo. After 2029 the signed and
  encrypted cases will fail certificate validation until the keystore is regenerated. Do not use it outside the demo.
- **One app, two connectors.** Both ports serve all paths (Mule bound `/services/*` to 63081 and `/client` to 63080).
- **No `?wsdl` publishing.** CXF served `?wsdl`; Spring-WS does not publish one here.
- **Empty `soap:Header`** that SAAJ adds to responses is removed, as CXF wrote none.
