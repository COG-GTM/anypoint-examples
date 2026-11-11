# Document integration using the CMIS connector (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`document-integration-using-the-cmis-connector`](../../document-integration-using-the-cmis-connector).

`/cmis` (any method) uploads `mulesoft-logo.jpg` to the CMIS folder `/okm:root/` as `pic<currentTimeMillis>.jpg` (`image/jpg`, `cmis:document`, versioning `NONE`) and returns the new object id as `{"id":"..."}`.

## Run

```bash
cd spring-boot-migration/document-integration-using-the-cmis-connector
mvn spring-boot:run -Dcmis.base-url=<atompub url> -Dcmis.username=<user> -Dcmis.password=<password>   # then: curl -X POST http://localhost:8081/cmis
```

## Tests

```bash
cd spring-boot-migration
mvn -pl document-integration-using-the-cmis-connector -am verify     # needs Docker for Testcontainers-based modules
```

The CMIS repository the Mule app used (the public OpenKM demo) can't be reproduced locally, so `CmisParityTest` replaces the typed `CmisClient` with a recording fake that answers like OpenKM (object id = folder path + file name). It asserts the HTTP response and the recorded call (`expectedCmisCall`: folder, file-name pattern, MIME type, object type, versioning state, content hash). The response id must be the id returned for that call. `OpenCmisClientTest` checks the AtomPub session parameters.

Fixtures live in `src/test/resources/parity/document-integration-using-the-cmis-connector/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- **Stubbed repository in tests**: the OpenCMIS AtomPub client is not exercised against a live CMIS server in CI; the parity tests use the typed-client stub described above.
- The Mule config hard-coded the public OpenKM demo account (`user3`/`pass3`). The credentials are now properties and blank by default; set `cmis.username`/`cmis.password`.
- OpenCMIS' Apache CXF dependencies (only needed for its Web Services binding) are excluded.
