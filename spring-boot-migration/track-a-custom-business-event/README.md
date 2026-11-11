# Track a custom business event: Spring Boot migration

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`track-a-custom-business-event`](../../track-a-custom-business-event).

`POST /customBusinessEvents` with a JSON order. It records a `Price` business event (item name, units, price per unit) and returns the discounted unit price (shoes 15 %, jeans 20 %, jackets 30 %) as text.

See [MIGRATION-NOTES.md](MIGRATION-NOTES.md) for the Mule construct → Spring mapping.

## Run

```bash
cd spring-boot-migration/track-a-custom-business-event
mvn spring-boot:run
```

```bash
curl -X POST -H 'Content-Type: application/json' -d '{"item name":"shoes","item units":2,"item price per unit":10}' http://localhost:8081/customBusinessEvents
```

## Tests

```bash
cd spring-boot-migration
mvn -pl track-a-custom-business-event -am verify
```

The parity fixtures under `src/test/resources/parity/track-a-custom-business-event/` describe the Mule app's behaviour. Each one cites its source in `source.reference`. They run against the Spring Boot app on every build, and also against a running Mule 3.8 app when `-Dparity.mule.base-url=...` is set (see [`parity-test-support`](../parity-test-support/README.md)).

## Behavioural deltas

- CloudHub Insight is replaced by Spring application events, a log line and a Micrometer counter. Events are not shipped to Anypoint.
- Unknown `item name`: the JRuby script left the global `$discount` unchanged. It then either reused the previous request's discount or failed with `nil` on the first request. Spring answers 500 on every request, so the result no longer depends on earlier requests.
