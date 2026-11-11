# sap-data-retrieval (Spring Boot 3)

Spring Boot port of [`sap-data-retrieval`](../../sap-data-retrieval): a read-only REST API (from `sap-api.raml`) over
three SAP BAPIs.

| Endpoint | Query parameters | BAPI | Response |
|---|---|---|---|
| `GET /api/customers` | `name`, `max_count` | `BAPI_CUSTOMER_FIND` (`MAX_CNT`, `PL_HOLD=X`, `KNA1.NAME1 = <name>*` or `*`) | `{"customers": [RESULT_TAB rows]}` |
| `GET /api/materials` | `name`, `max_count` | `BAPI_MATERIAL_GETLIST` (`MAXROWS`, `I CP <name>*` or `*`) | `{"materials": [MATNRLIST rows]}` or `{"materials": {"message": "No data found."}}` |
| `GET /api/salesOrders` | `customer_number`, `sales_organization` (RAML default `" "`) | `BAPI_SALESORDER_GETLIST` | `{"salesOrders": [SALES_ORDERS rows]}` or `{"salesOrders": {"message": "No data found."}}` |

APIkit errors are kept: 404 `{"message": "Resource not found"}`, 405 `{"message": "Method not allowed"}`, 415
`{"message": "Unsupported media type"}`.

The three Mule `sap:outbound-endpoint type="function"` calls are replaced by the typed `SapClient` interface
(`findCustomers`, `getMaterials`, `getSalesOrders`, each with a request record from `SapRequests`, returning
`List<SapRow>`). The bundled implementation, `SapGatewayClient`, is a `WebClient` client that POSTs the connector's
`xmlVersion="2"` BAPI document (the same XML the DataWeave scripts built) to `/functions/{BAPI}` and parses the result
table from the XML reply.

## Run

```bash
cd spring-boot-migration
mvn -B -pl sap-data-retrieval -am package -DskipTests
SAP_GATEWAY_BASE_URL=https://<bapi-gateway> SAP_JCO_CLIENT=800 SAP_JCO_USER=... SAP_JCO_PASSWD=... \
  java -jar sap-data-retrieval/target/sap-data-retrieval-1.0.0-SNAPSHOT.jar
curl 'http://localhost:8081/api/materials?name=Steel&max_count=10'
```

| Property | Env var | Default |
|---|---|---|
| `server.port` | `-Dhttp.port` | `8081` |
| `sap.gateway-base-url` | `SAP_GATEWAY_BASE_URL` | `https://${sap.jco.ashost}:44300/sap/bapi` |
| `sap.jco.ashost` / `sap.jco.sysnr` | `SAP_JCO_ASHOST` / `SAP_JCO_SYSNR` | `localhost` / `00` |
| `sap.jco.client` / `sap.jco.lang` | `SAP_JCO_CLIENT` / `SAP_JCO_LANG` | `800` / `EN` |
| `sap.jco.user` / `sap.jco.passwd` | `SAP_JCO_USER` / `SAP_JCO_PASSWD` | empty |

## Parity tests

`SapDataRetrievalParityTest` (`AbstractWireMockParityTest`) runs the 9 fixtures in
`src/test/resources/parity/sap-data-retrieval/`. WireMock plays the SAP gateway and serves the Mule app's
`sample_data/BAPI_*.xml` replies, plus a two-row reply and empty-table replies. The fixtures assert:

- the exact outbound BAPI XML (compared as XML), Basic auth and `sap-client`/`sap-language` headers;
- the normalised JSON (strict);
- the `No data found.` branches;
- APIkit 404/405, with no SAP call.

```bash
cd spring-boot-migration && mvn -B -pl sap-data-retrieval -am verify
```

## Behavioural deltas

1. **SAP transport (proprietary JCo).** The Mule SAP connector calls the BAPIs over RFC using the proprietary SAP JCo
   library, which can't be redistributed or reproduced here. The port puts the calls behind the `SapClient`
   interface. The bundled `SapGatewayClient` sends the identical BAPI XML over HTTP to a gateway that executes the
   function, authenticating with the same JCo user/password/client/language. To call SAP directly, provide a
   JCo-backed `SapClient` bean. `sap.jco.ashost`/`sysnr` only feed the default gateway URL.
2. **Missing `max_count`.** DataWeave would emit `MAX_CNT`/`MAXROWS` from a null query parameter. The port writes an
   empty element (`<MAX_CNT/>`). No fixture covers the parameter-less case, because the Mule output for it isn't
   documented.
3. **Empty customer result.** Mule's customers DataWeave has no `otherwise` branch, so an empty `RESULT_TAB` behaviour
   is undocumented. The port returns `{"customers": []}`; no fixture asserts it.
4. **APIkit console / 406 / non-`/api` paths.** These behave as in `netsuite-data-retrieval`: no `/api/console`
   (springdoc serves `/v3/api-docs` and `/swagger-ui.html` instead);
   Spring's empty-body 406; and APIkit-style 404 JSON for every unknown path.
5. **`/salesOrder` flow mapping.** The Mule `apikit:flow-mapping resource="/salesOrder"` names a resource that isn't in
   the RAML. The flow is reached by APIkit's naming convention (`get:/salesOrders:sap-api-config`) at the RAML path
   `/salesOrders`, which is what the port serves.
