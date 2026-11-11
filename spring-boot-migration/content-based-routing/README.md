# content-based-routing (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`content-based-routing`](../../content-based-routing).

Greets in the language named by the `language` query parameter: `Spanish` -> `Hola!`, `French` -> `Bonjour!`, anything else -> `Hello!` (logged as English).

## Run

```bash
cd spring-boot-migration
mvn -B -pl content-based-routing -am verify          # build + parity tests
mvn -pl content-based-routing spring-boot:run        # then: GET http://localhost:8081/?language=Spanish
```

## Tests

- Parity fixtures: `src/test/resources/parity/content-based-routing/` (6 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `ContentBasedRoutingParityTest`, `ContentBasedRoutingTableTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

None.
