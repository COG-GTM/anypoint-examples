# netsuite-data-retrieval (Spring Boot 3)

Spring Boot port of [`netsuite-data-retrieval`](../../netsuite-data-retrieval): a read-only REST API (from
`netsuite-api.raml`) over NetSuite.

| Endpoint | Query parameters | NetSuite search |
|---|---|---|
| `GET /api/customers` | `name` (optional) | `CUSTOMER`, `basic.companyName STARTS_WITH name` |
| `GET /api/items` | `quantity` (integer, default `0`), `operator` (enum, default `GREATER_THAN`) | `ITEM_SUPPLY_PLAN_BASIC`, `quantity <operator> quantity` |
| `GET /api/opportunities` | `title` (optional) | `OPPORTUNITY`, `basic.title STARTS_WITH title` |

Each endpoint returns the found records as a JSON array. APIkit errors are kept: 404 `{"message": "Resource not found"}`,
405 `{"message": "Method not allowed"}`, 415 `{"message": "Unsupported media type"}`, 400 `{"message": "Bad request"}`
(invalid `quantity`/`operator`).

The Mule NetSuite connector (`netsuite:search`) is replaced by `NetSuiteSearchClient`, a typed `WebClient` client. Search
criteria are typed records that mirror the SuiteTalk classes the DataWeave scripts built (`CustomerSearch`,
`ItemSupplyPlanSearchBasic`, `OpportunitySearch`, `SearchStringField`, `SearchLongField`); they're POSTed as JSON to
`/search/{searchRecord}`, and the records come back as `NetSuiteRecord` (every field kept, in order).

## Run

```bash
cd spring-boot-migration
mvn -B -pl netsuite-data-retrieval -am package -DskipTests
NETS_BASE_URL=https://<your-netsuite-gateway> NETS_ACCOUNT=... NETS_EMAIL=... NETS_PASSWORD=... NETS_ROLE_ID=... NETS_APPLICATION_ID=... \
  java -jar netsuite-data-retrieval/target/netsuite-data-retrieval-1.0.0-SNAPSHOT.jar
curl 'http://localhost:8081/api/items?quantity=5&operator=LESS_THAN'
```

| Property | Env var | Default |
|---|---|---|
| `server.port` | `-Dhttp.port` | `8081` |
| `nets.base-url` | `NETS_BASE_URL` | `https://${nets.account}.suitetalk.api.netsuite.com` |
| `nets.email` / `nets.password` / `nets.account` / `nets.role-id` / `nets.application-id` | `NETS_EMAIL` / `NETS_PASSWORD` / `NETS_ACCOUNT` / `NETS_ROLE_ID` / `NETS_APPLICATION_ID` | empty |

## Parity tests

`NetsuiteDataRetrievalParityTest` (`AbstractWireMockParityTest`) runs the 10 fixtures in
`src/test/resources/parity/netsuite-data-retrieval/`. WireMock plays NetSuite and serves the RAML example responses;
each fixture asserts the exact outbound search criteria JSON and `NLAuth` header, and that the API returns the same
records (strict JSON). Error fixtures assert the APIkit status/body and that NetSuite isn't called.

```bash
cd spring-boot-migration && mvn -B -pl netsuite-data-retrieval -am verify
```

## Behavioural deltas

1. **Transport to NetSuite.** The Mule connector calls the SuiteTalk SOAP `search` operation through the proprietary
   NetSuite SDK. That SDK isn't reproducible here, so `NetSuiteSearchClient` sends a JSON projection of the same search
   object to `/search/{CUSTOMER|ITEM_SUPPLY_PLAN_BASIC|OPPORTUNITY}` with `NLAuth` credentials built from the same
   `nets.*` properties. In production, point `nets.base-url` at a SuiteTalk/RESTlet bridge that exposes that contract
   (or swap in a SOAP implementation behind the same client methods).
2. **APIkit console.** `/api/console` isn't served; springdoc publishes the OpenAPI document at `/v3/api-docs` and
   Swagger UI at `/swagger-ui.html` instead.
3. **406 Not Acceptable.** Spring's content negotiation returns 406 with an empty body, not `{"message": "Not acceptable"}`.
4. **Paths outside `/api`.** Mule only listened on `/api/*`; the port answers any unknown path with the APIkit-style 404
   JSON instead of the Mule listener's plain-text 404.
5. **RAML example.** `opportunities-response.json` contains raw newlines inside `addrText`, which makes it invalid JSON;
   the fixture copy escapes them (`\n`). The data is otherwise unchanged.
