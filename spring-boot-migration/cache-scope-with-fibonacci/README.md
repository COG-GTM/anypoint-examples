# cache-scope-with-fibonacci (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`cache-scope-with-fibonacci`](../../cache-scope-with-fibonacci).

GET `/fibonacci?n=N` computes Fibonacci(N) recursively through a cache scope and reports the number of computations (`COST`). `n >= 20` is rejected.

## Run

```bash
cd spring-boot-migration
mvn -B -pl cache-scope-with-fibonacci -am verify          # build + parity tests
mvn -pl cache-scope-with-fibonacci spring-boot:run        # then: GET http://localhost:8081/fibonacci?n=10
```

## Tests

- Parity fixtures: `src/test/resources/parity/cache-scope-with-fibonacci/` (7 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `CacheScopeWithFibonacciParityTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The cache is in memory (`spring.cache.type: simple`), like Mule's default in-memory store. Mule's TTL and expiry settings are not reproduced; entries live as long as the JVM.
