# Migration notes: `soap-webservice-security`

- Original Mule app: [`../../soap-webservice-security`](../../soap-webservice-security)
- Mule flows: `mule-config.xml` → `UnsecureServiceFlow`, `UsernameTokenServiceFlow`, `UsernameTokenSignedServiceFlow`, `UsernameTokenEncryptedServiceFlow`, `SamlTokenServiceFlow`, `SignedSamlTokenServiceFlow`; `service-clients.xml` → `SecurityClients` + six client sub-flows
- Spring Boot module: `spring-boot-migration/soap-webservice-security`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener-config port="63081" basePath="services"` | `server.port: ${http.port:63081}`, servlet mappings `/services/<path>` | Boot's `WebServicesAutoConfiguration` excluded |
| `http:listener-config port="63080"` | `ClientConnectorConfiguration` (extra Tomcat connector) | `security.client.port` |
| `http:listener path="unsecure"…"signedsaml"` | `WebServiceConfiguration#messageDispatcherServlet` | |
| `cxf:jaxws-service serviceClass="Greeter"` + `component GreeterService` | `GreeterEndpoint` (`@Endpoint`, `@PayloadRoot greet`) | namespace `http://security.example.mule.mulesoft.com/` |
| `cxf:ws-security` / `ws-config action=...` | `MuleWss4jInterceptor` (`Wss4jSecurityInterceptor`) per path via `PathScopedInterceptor` | |
| `passwordCallbackRef` `PasswordCallback` | `PasswordCallback` (WSS4J 2 `WSPasswordCallback`) | |
| `signaturePropFile` / `decryptionPropFile` `wssecurity.properties` | `WebServiceConfiguration#wssCrypto` (Merlin, `keystore.jks`) | |
| `ws-custom-validator saml2-token-validator` `SAMLCustomValidator` | `SamlCustomValidator` (`SamlAssertionValidator`) via `WSSConfig#setValidator` | |
| CXF security fault | `MuleWss4jInterceptor#handleValidationException` | SOAP 1.1 fault with WSS4J fault code |
| `SecurityClients` `choice` on `clientType` | `SecurityClientController` + `SecurityClients#forType` | |
| `cxf:jaxws-client` + `cxf:ws-security` (client sub-flows) | `WebServiceTemplate` + client `Wss4jSecurityInterceptor` (`SecurityClients`) | |
| `samlCallbackHandler` `SAMLCallbackHandler` + `saml.properties` | `SamlCallbackHandler` (issuer, issuer crypto joe/secret) | |
| `http:request` `HTTP_Request_Configuration` | `security.services.base-url` | |
| `catch-exception-strategy` | `try/catch` in `SecurityClientController` | same text payload |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| listener port `63081` | `server.port` (`${http.port:63081}`) | 63081 |
| client listener port `63080` | `security.client.port` | 63080 |
| `wssecurity.properties` | `security.keystore.*` | `keystore.jks`, `keyStorePassword`, `joe` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-unsecure-greet` | plain greet | documented (IT `testUnsecure`) |
| `02-username-token` | UsernameToken + Timestamp accepted | documented (IT) |
| `03-username-token-wrong-password` | wrong password → FailedAuthentication fault | documented (IT) |
| `04-username-token-signed` | signed request accepted | documented (IT) |
| `05-signed-service-rejects-unsigned` | unsigned → `<wsse:Security> header` fault | documented (IT) |
| `06-username-token-encrypted` | encrypted body decrypted and accepted | documented (IT) |
| `07-saml-token` | unsigned SAML 2 SV accepted by custom validator | documented (IT) |
| `08-saml-token-wrong-issuer` | wrong issuer → `<wsse:Security> header` fault | documented (IT) |
| `09-signed-saml-token` | SAMLTokenSigned accepted | documented (IT) |
| `10`–`15-client-*` | `/client` for each `clientType` → `Hello Mule` text/plain | documented (flow XML) |
| `16-client-other` | unknown clientType → `Client type is not supported` | documented (flow XML) |
