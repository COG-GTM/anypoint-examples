# get-customer-list-from-netsuite (Spring Boot 3)

Spring Boot port of [`get-customer-list-from-netsuite`](../../get-customer-list-from-netsuite). `GET /customers?lastName=<prefix>`
queries NetSuite for customers whose last name starts with the prefix (ordered by last name) and returns the
`customer/index.html` page with one table row (last name, first name, email) per customer, or a single
`No customers found` row.

The Mule NetSuite connector (`netsuite:query`) is replaced by `NetSuiteQueryClient`, a typed `WebClient` that POSTs the
same DSQL query string to NetSuite's SuiteQL REST endpoint (`/services/rest/query/v1/suiteql`, `Prefer: transient`) and
maps the rows onto `NetSuiteCustomer(email, firstName, lastName)`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl get-customer-list-from-netsuite -am package -DskipTests
NETSUITE_ACCOUNT=... NETSUITE_EMAIL=... NETSUITE_PASSWORD=... NETSUITE_ROLE_ID=... \
  java -jar get-customer-list-from-netsuite/target/get-customer-list-from-netsuite-1.0.0-SNAPSHOT.jar
curl 'http://localhost:8081/customers?lastName=a'
```

| Property | Env var | Default |
|---|---|---|
| `server.port` | `HTTP_PORT` / `-Dhttp.port` | `8081` |
| `netsuite.base-url` | `NETSUITE_BASE_URL` | `https://${netsuite.account}.suitetalk.api.netsuite.com` |
| `netsuite.email` / `netsuite.password` / `netsuite.account` / `netsuite.role-id` | `NETSUITE_EMAIL` / `NETSUITE_PASSWORD` / `NETSUITE_ACCOUNT` / `NETSUITE_ROLE_ID` | empty |

## Parity tests

`GetCustomerListFromNetsuiteParityTest` (`AbstractWireMockParityTest`) runs the fixtures in
`src/test/resources/parity/get-customer-list-from-netsuite/`. WireMock plays NetSuite; each fixture asserts the exact
outbound query body and `NLAuth` header, the HTTP status, content type and the rendered HTML (matched tag by tag in document order).

```bash
cd spring-boot-migration && mvn -B -pl get-customer-list-from-netsuite -am verify
```

## Behavioural deltas

1. **Transport to NetSuite.** The Mule connector speaks SuiteTalk SOAP through the proprietary NetSuite connector; the
   port sends the identical DSQL string to the SuiteQL REST endpoint with `NLAuth` credentials built from the same four
   `netsuite.*` properties. SuiteQL accepts this SELECT/WHERE/ORDER BY form, but production accounts that require
   token-based authentication (OAuth 1.0 TBA) need an `ExchangeFilterFunction` on the `WebClient`; it isn't included.
2. **Quote escaping.** Mule concatenated `lastName` into the query unescaped (DSQL injection). The port doubles single
   quotes (`O'Brien` → `O''Brien`). Queries without `'` are byte-identical.
3. **Template rendering.** The Groovy `parse-template` expression is replaced by a string substitution of the same
   `<div><line><tr><td>…` fragment (2-space indentation, XML declaration line dropped). The fixtures match the whole
   page tag by tag, in document order (rows and cells can't be reordered). They don't assert whitespace between tags,
   because the exact indentation of DataWeave's XML writer isn't documented. In live Mule-vs-Spring mode the harness
   compares the two pages literally.
