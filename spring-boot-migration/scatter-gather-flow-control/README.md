# scatter-gather-flow-control (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`scatter-gather-flow-control`](../../scatter-gather-flow-control).

GET `/scatterGather` reads contacts from two sources in parallel, merges them by email, turns them into CSV and emails the report as an attachment.

## Run

```bash
cd spring-boot-migration
mvn -B -pl scatter-gather-flow-control -am verify          # build + parity tests
mvn -pl scatter-gather-flow-control spring-boot:run        # then: GET http://localhost:8081/scatterGather
```

## Tests

- Parity fixtures: `src/test/resources/parity/scatter-gather-flow-control/` (2 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `ContactMergeTableTest`, `ScatterGatherFlowControlParityTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The Gmail connector became plain SMTP (`spring.mail.*`). Parity tests use a GreenMail server instead of Gmail.
- Line endings in the attachment are compared after turning CRLF into LF, because SMTP sends CRLF.
