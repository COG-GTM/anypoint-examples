# dataweave-with-flowreflookup (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`dataweave-with-flowreflookup`](../../dataweave-with-flowreflookup).

Polls `companies.csv`, maps each row to a Salesforce Account (the region comes from a flow-ref lookup by state), creates the accounts in Salesforce and moves the file to `output`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl dataweave-with-flowreflookup -am verify          # build + parity tests
mvn -pl dataweave-with-flowreflookup spring-boot:run        # then: drop a CSV into the input directory
```

## Tests

- Parity fixtures: `src/test/resources/parity/dataweave-with-flowreflookup/` (3 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `DataweaveWithFlowreflookupFileParityTest`, `SalesRegionLookupTableTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The Mule Salesforce connector (username/password/security token) became REST calls to `/services/data/{version}/composite/sobjects` with an OAuth bearer token (`salesforce.access-token`). Tests run it against WireMock.
- The app has no HTTP endpoint, same as the Mule app. Parity fixtures use `method: FILE` and drop the CSV into the input folder.
