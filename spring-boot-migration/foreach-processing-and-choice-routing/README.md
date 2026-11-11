# foreach-processing-and-choice-routing (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`foreach-processing-and-choice-routing`](../../foreach-processing-and-choice-routing).

Loan broker: `GET /?name=..&ssn=..&amount=..&term=..` gets a credit profile, picks banks by amount, asks each one for a quote (foreach) and returns the lowest as `Bank #N, rate: R`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl foreach-processing-and-choice-routing -am verify          # build + parity tests
mvn -pl foreach-processing-and-choice-routing spring-boot:run        # then: GET http://localhost:11081/?name=Ann&ssn=1234&amount=25000&term=12
```

## Tests

- Parity fixtures: `src/test/resources/parity/foreach-processing-and-choice-routing/` (9 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `BankRoutingTableTest`, `ForeachProcessingAndChoiceRoutingParityTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The credit agency and banks are called with JSON over HTTP, not SOAP/CXF. These were internal mocks in the Mule app, and the public HTTP contract on port 11081 is the same.
- The mock services run on the app's own port under `/mule/...`, not on ports 18080 and 10080-50080.
- Mock credit scores and bank rates are still random, so parity fixtures stub them with WireMock to get fixed results.
