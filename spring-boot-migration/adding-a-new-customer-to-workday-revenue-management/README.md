# adding-a-new-customer-to-workday-revenue-management (Spring Boot 3)

Spring Boot port of
[`adding-a-new-customer-to-workday-revenue-management`](../../adding-a-new-customer-to-workday-revenue-management).
`POST /` with an XML `<root><Account>…</Account></root>` body creates the customer in Workday Revenue Management
(`Put_Customer`) and replies `Added customer: <Workday customer descriptor>`.

The Mule Workday connector (`wd-connector:invoke type="Revenue_Management||Put_Customer"`) is replaced by
`WorkdayRevenueManagementClient`. It's a typed `WebClient` client: it takes a `PutCustomerRequest`, writes the same
`urn:com.workday/bsvc` `Put_Customer_Request` the DataWeave script produced, wraps it in a SOAP 1.1 envelope with a
WS-Security `UsernameToken` (`<user>@<tenant>`), POSTs it to
`https://<hostname>/ccx/service/<tenant>/Revenue_Management/<version>`, and returns a `PutCustomerResponse` holding
`Customer_Reference/@wd:Descriptor`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl adding-a-new-customer-to-workday-revenue-management -am package -DskipTests
WDAY_HOSTNAME=wd2-impl-services1.workday.com WDAY_TENANT=... WDAY_USER=... WDAY_PASSWORD=... \
  java -jar adding-a-new-customer-to-workday-revenue-management/target/adding-a-new-customer-to-workday-revenue-management-1.0.0-SNAPSHOT.jar
curl -X POST -H 'Content-Type: application/xml' --data @customer.xml http://localhost:9090/
```

| Property | Env var | Default |
|---|---|---|
| `server.port` | `-Dhttp.port` | `9090` (port used in the Mule README) |
| `wday.hostname` | `WDAY_HOSTNAME` | `wd2-impl-services1.workday.com` |
| `wday.base-url` | `WDAY_BASE_URL` | `https://${wday.hostname}` |
| `wday.tenant` / `wday.user` / `wday.password` | `WDAY_TENANT` / `WDAY_USER` / `WDAY_PASSWORD` | empty |
| `wday.version` | `WDAY_VERSION` | `v25.1` |

## Parity tests

`AddNewCustomerToWorkdayParityTest` (`AbstractWireMockParityTest`) runs the 5 fixtures in
`src/test/resources/parity/adding-a-new-customer-to-workday-revenue-management/`. WireMock plays the Workday SOAP
endpoint. The fixtures assert:

- the exact outbound SOAP envelope (compared as XML, namespace-aware);
- the reply text and status;
- 405 for non-POST requests, and no Workday call;
- 500 on a Workday SOAP fault;
- 500 on malformed input XML, and no Workday call.

```bash
cd spring-boot-migration && mvn -B -pl adding-a-new-customer-to-workday-revenue-management -am verify
```

## Behavioural deltas

1. **Workday transport.** The proprietary Workday connector builds the SOAP call internally. The port builds the
   envelope itself, so the body content is the DataWeave mapping, and the WS-Security header and endpoint path follow
   Workday's public web-service conventions. The API version is now explicit (`wday.version`); the connector pinned it
   per release.
2. **Error bodies.** Fault and malformed-XML cases return 500, as in Mule. The body is Spring Boot's JSON error, not
   Mule's exception text. Fixtures assert only the status.
3. **Missing descriptor.** If Workday's response has no `Customer_Reference/@Descriptor`, the reply is
   `Added customer: null`. That matches DataWeave's null rendering, but no fixture asserts it.
