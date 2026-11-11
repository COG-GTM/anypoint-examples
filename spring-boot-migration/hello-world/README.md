# Hello World: Spring Boot migration

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`hello-world`](../../hello-world).

Answers `/helloWorld` (any HTTP method) with the constant text `Hello World`.

See [MIGRATION-NOTES.md](MIGRATION-NOTES.md) for the Mule construct → Spring mapping.

## Run

```bash
cd spring-boot-migration/hello-world
mvn spring-boot:run
```

```bash
curl http://localhost:8081/helloWorld
```

## Tests

```bash
cd spring-boot-migration
mvn -pl hello-world -am verify
```

The parity fixtures under `src/test/resources/parity/hello-world/` describe the Mule app's behaviour. Each one cites its source in `source.reference`. They run against the Spring Boot app on every build, and also against a running Mule 3.8 app when `-Dparity.mule.base-url=...` is set (see [`parity-test-support`](../parity-test-support/README.md)).

## Behavioural deltas

None.
