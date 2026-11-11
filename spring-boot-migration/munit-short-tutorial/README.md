# MUnit short tutorial: Spring Boot migration

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`munit-short-tutorial`](../../munit-short-tutorial).

`GET /?url_key=payload_1` answers `response_payload_1`; any other (or missing) `url_key` answers `response_payload_2`. The MUnit suite is ported to JUnit 5 + Mockito.

See [MIGRATION-NOTES.md](MIGRATION-NOTES.md) for the Mule construct → Spring mapping.

## Run

```bash
cd spring-boot-migration/munit-short-tutorial
mvn spring-boot:run
```

```bash
curl 'http://localhost:9090/?url_key=payload_1'
```

## Tests

```bash
cd spring-boot-migration
mvn -pl munit-short-tutorial -am verify
```

The parity fixtures under `src/test/resources/parity/munit-short-tutorial/` describe the Mule app's behaviour. Each one cites its source in `source.reference`. They run against the Spring Boot app on every build, and also against a running Mule 3.8 app when `-Dparity.mule.base-url=...` is set (see [`parity-test-support`](../parity-test-support/README.md)).

## Behavioural deltas

- Rejected methods (e.g. `POST /`) return 405 in both, but the body differs: Spring Boot's JSON error document vs Mule's plain-text listener message. The fixture asserts only the status.
