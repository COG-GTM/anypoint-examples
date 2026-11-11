# service-orchestration-and-choice-routing (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`service-orchestration-and-choice-routing`](../../service-orchestration-and-choice-routing).

SOAP order service: for each item, Samsung products are ordered from the Samsung SOAP service and others are fulfilled in-house (cached price lookup + database insert). Each item gets a purchase receipt, and the order total is audited asynchronously. Also has `/populate` and the AJAX demo channels.

## Run

```bash
cd spring-boot-migration
mvn -B -pl service-orchestration-and-choice-routing -am verify          # build + parity tests
mvn -pl service-orchestration-and-choice-routing spring-boot:run        # then: POST a SOAP envelope (see src/test/resources/parity) to http://localhost:1080/orders
```

## Tests

- Parity fixtures: `src/test/resources/parity/service-orchestration-and-choice-routing/` (10 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `OrderRoutingTableTest`, `OrderSideEffectsTest`, `ServiceOrchestrationAndChoiceRoutingParityTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- All of Mule's listeners (1080, 8091, 9090, 9999) share one port, by default 1080.
- The AJAX (CometD) channels on port 8090 are now plain HTTP endpoints, and the bundled `docroot` demo page is not ported.
- JMS/XA transactions and ActiveMQ became one local `@Transactional` call. JMS redelivery (`maxRedeliveryAttempts=3`) is not reproduced; an in-house failure returns a SOAP fault (500).
- Derby runs in memory, not on disk.
- Mule's `priceService` listener allows only POST, even though `inhouseOrder` calls it with GET. The bundled price service here answers GET so the in-house branch works.
- `totalValue` is added up as a number. MEL coerced the Samsung `price` string.
- The Mule IT's `reply.xml` is empty, and `sample-response.json` copies the Samsung receipt onto the Philips item. Fixtures follow the flow logic and the `summary.xml` shape instead.
