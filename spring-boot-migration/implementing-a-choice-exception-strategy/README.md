# Implementing a choice exception strategy: Spring Boot migration

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`implementing-a-choice-exception-strategy`](../../implementing-a-choice-exception-strategy).

`POST /` with a JSON order. It returns `Input data validation passed.` for valid input, and a 400 with `Invalid input data: …` (`IllegalArgumentException`) or `Missing input data: …` (`NullPointerException`) for faulty input, chosen in the same way as the Mule `choice-exception-strategy`.

See [MIGRATION-NOTES.md](MIGRATION-NOTES.md) for the Mule construct → Spring mapping.

## Run

```bash
cd spring-boot-migration/implementing-a-choice-exception-strategy
mvn spring-boot:run
```

```bash
curl -i -X POST -H 'Content-Type: application/json' -d '{"item name":"aa","item units":10,"item price per unit":1,"membership":"free"}' http://localhost:8081/
```

## Tests

```bash
cd spring-boot-migration
mvn -pl implementing-a-choice-exception-strategy -am verify
```

The parity fixtures under `src/test/resources/parity/implementing-a-choice-exception-strategy/` describe the Mule app's behaviour. Each one cites its source in `source.reference`. They run against the Spring Boot app on every build, and also against a running Mule 3.8 app when `-Dparity.mule.base-url=...` is set (see [`parity-test-support`](../parity-test-support/README.md)).

## Behavioural deltas

- The custom HTTP reason phrases (`Invalid input data` / `Missing input data`) are not sent. Tomcat (Spring Boot's server) always writes the standard reason phrase, and HTTP/2 has none. Status code and body are unchanged.
- `#[payload]` printed a `java.util.HashMap`. The README's documented output follows the JDK 7 `HashMap` iteration order, so `MulePayloadRenderer` reproduces that order exactly. The same Mule app running on JDK 8+ would print the keys in a different order.
- Only `POST` is mapped. The Mule listener accepted any method, but without a JSON body the transformer failed, so other methods were never useful.
