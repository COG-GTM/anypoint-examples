# import-contacts-into-ms-dynamics (Spring Boot 3)

Spring Boot port of [`import-contacts-into-ms-dynamics`](../../import-contacts-into-ms-dynamics). The app polls an input
directory every 10 seconds. Each CSV file found (header `firstname,surname,phone,email`) is moved to the output
directory and every row becomes a Dynamics CRM `contact` (`firstname`, `lastname` ← `surname`, `emailaddress1` ←
`email`, `telephone1` ← `phone`), created one at a time in row order.

- The Mule `file:inbound-endpoint` is a Spring Integration `Files.inboundAdapter` with a fixed-delay poller
  (`ContactImportFlowConfig`).
- The DataWeave "CSV To Map" step is Jackson CSV → `CsvContact` → `ContactRecord`.
- `foreach` + `dynamicscrm:create` is a loop over `DynamicsCrmClient#createContact`, a typed `WebClient` client that
  POSTs `DynamicsContact` to the Dynamics 365 Web API (`/api/data/v9.2/contacts`).

## Run

```bash
cd spring-boot-migration
mvn -B -pl import-contacts-into-ms-dynamics -am package -DskipTests
cd import-contacts-into-ms-dynamics
CRM_BASE_URL=https://yourorg.crm.dynamics.com CRM_ACCESS_TOKEN=<oauth token> \
  java -jar target/import-contacts-into-ms-dynamics-1.0.0-SNAPSHOT.jar
cp src/main/resources/contacts.csv src/main/resources/input/
```

| Property | Env var | Default |
|---|---|---|
| `contacts.input-dir` | `CONTACTS_INPUT_DIR` | `src/main/resources/input` |
| `contacts.output-dir` | `CONTACTS_OUTPUT_DIR` | `src/main/resources/output` |
| `contacts.polling-frequency` (ms) | | `10000` |
| `crm.base-url` | `CRM_BASE_URL` | `https://yourorg.crm.dynamics.com` |
| `crm.api-version` | | `v9.2` |
| `crm.access-token` | `CRM_ACCESS_TOKEN` | empty (no `Authorization` header) |

## Parity tests

This app has no HTTP listener, so `ImportContactsIntoMsDynamicsParityTest` drives the file endpoint directly. The test
uses `ParityFixtures` and `WireMockParitySupport` from `parity-test-support`, without `AbstractParityTest`. For each
fixture in `src/test/resources/parity/import-contacts-into-ms-dynamics/` it:

1. writes `request.body` into a temporary input directory as `request.path`;
2. waits for the file to be moved to the output directory;
3. asserts the Dynamics calls WireMock received against `expectedOutbound`, in CSV row order: method, path,
   `Authorization`/`Content-Type` headers and the exact JSON attributes;
4. fails if there are any extra or unstubbed calls.

```bash
cd spring-boot-migration && mvn -B -pl import-contacts-into-ms-dynamics -am verify
```

## Behavioural deltas

1. **Dynamics protocol and authentication.** The Mule connector used the legacy Organization Service (SOAP) with
   `CRMONLINE` username/password authentication. Microsoft has retired that authentication method, and the connector
   SDK is proprietary. The port calls the Dynamics 365 Web API (OData v4) with an OAuth 2.0 bearer token
   (`crm.access-token`). `crm.username`, `crm.password`, `crm.auth-type` are therefore not used, and `crm.service-url` is
   replaced by `crm.base-url` (the organisation root URL). The entity (`contact`) and the four attribute names and
   values are unchanged.
2. **Directories are configurable.** Mule hard-coded `src/main/resources/input|output`. Those paths are kept as the
   defaults and can be overridden.
3. **Failure handling.** As in Mule, the file is moved once it has been read, before the contacts are created. If a
   create fails, the remaining rows of that file are skipped and the error is logged. Mule's default exception strategy
   behaves the same way. No fixture asserts this behaviour, because the Mule app documents no error contract.
