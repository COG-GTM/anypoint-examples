# filtering-a-message (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`filtering-a-message`](../../filtering-a-message).

POST a JSON membership record to `/`; a custom filter grants the discount (`the discount was granted.`) or silently drops the message (empty 200).

## Run

```bash
cd spring-boot-migration
mvn -B -pl filtering-a-message -am verify          # build + parity tests
mvn -pl filtering-a-message spring-boot:run        # then: curl -X POST localhost:8081/ -d '{"membership":"premium","months":6,"purchases":1000}'
```

## Tests

- Parity fixtures: `src/test/resources/parity/filtering-a-message/` (7 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `FilteringAMessageParityTest`, `FilteringRoutingTableTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

None.
