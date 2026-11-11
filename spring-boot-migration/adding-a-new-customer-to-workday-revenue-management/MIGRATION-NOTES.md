# Migration notes: `adding-a-new-customer-to-workday-revenue-management`

- Original Mule app: [`../../adding-a-new-customer-to-workday-revenue-management`](../../adding-a-new-customer-to-workday-revenue-management)
- Mule flows: `add_a_new_customer.xml` → `add-customer-flow`
- Spring Boot module: `spring-boot-migration/adding-a-new-customer-to-workday-revenue-management`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/" allowedMethods="POST"` | `AddCustomerController#addCustomer` (`@PostMapping("/")`) | Other methods → 405. |
| `mulexml:xml-to-dom-transformer` | `Xml.parse(body, false)` (DOM, DTDs disabled) | |
| `dw:transform-message` "XML to Workday PutCustomerRequest XML" | `AddCustomerController#toPutCustomerRequest` → `PutCustomerRequest` + `WorkdayRevenueManagementClient#envelope` (StAX, `ns0 = urn:com.workday/bsvc`) | Same element tree and `ns0:type` attributes. |
| `wd-connector:config` (hostName/tenantName/username/password) | `WorkdayProperties` (`wday.*`) + WS-Security `UsernameToken` | |
| `wd-connector:invoke type="Revenue_Management\|\|Put_Customer"` | `WorkdayRevenueManagementClient#putCustomer` (WebClient POST `/ccx/service/{tenant}/Revenue_Management/{version}`) | |
| `dw:transform-message` "Extract Customer Name" (`Customer_Reference.@Descriptor`) | `WorkdayRevenueManagementClient#descriptor` → `PutCustomerResponse` | |
| `set-payload value="Added customer: #[payload]"` | `"Added customer: " + descriptor` (`text/plain`) | |
| default exception strategy | Spring Boot error handling → 500 | Delta 2. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:9090}`) | 9090 |
| `wday.hostname` | `wday.hostname` (→ `wday.base-url`) | `wd2-impl-services1.workday.com` |
| `wday.tenant` | `wday.tenant` | empty |
| `wday.user` | `wday.user` | empty |
| `wday.password` | `wday.password` | empty |
| (connector-internal API version) | `wday.version` | `v25.1` |

## Parity coverage

| Fixture | What it proves | Evidence |
|---|---|---|
| `01-add-customer-readme-example` | README request → exact `Put_Customer_Request` envelope; `Added customer: John Doe` | documented |
| `02-add-customer-descriptor-from-workday` | entity name mapped separately; reply uses Workday's descriptor | documented |
| `03-get-not-allowed` | `allowedMethods="POST"` → 405, no Workday call | documented |
| `04-workday-soap-fault` | SOAP fault → 500 | documented |
| `05-malformed-xml` | bad input XML → 500, no Workday call | documented |

## Behavioural deltas

See [README](README.md#behavioural-deltas).
