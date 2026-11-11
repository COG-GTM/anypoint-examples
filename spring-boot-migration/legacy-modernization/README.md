# Legacy modernization: Spring Boot migration

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`legacy-modernization`](../../legacy-modernization).

SOAP 1.1 web service `putShippingOrder` (namespace `http://ordermgmt.org/`) on `POST http://localhost:1080/OrderFulfillment`. It echoes the order in a `ShippingOrderConfirmation` with `orderReceivedStatus=true` and asynchronously writes the legacy CSV file `ShippingOrder-dd-MM-yy_HH-mm-ss.SSS.txt`, one record per order item.

See [MIGRATION-NOTES.md](MIGRATION-NOTES.md) for the Mule construct → Spring mapping.

## Run

```bash
cd spring-boot-migration/legacy-modernization
mvn spring-boot:run
```

```bash
curl -X POST -H 'Content-Type: text/xml' --data @../../legacy-modernization/src/test/resources/message.xml http://localhost:1080/OrderFulfillment
ls src/main/resources/Output
```

## Tests

```bash
cd spring-boot-migration
mvn -pl legacy-modernization -am verify
```

The parity fixtures under `src/test/resources/parity/legacy-modernization/` describe the Mule app's behaviour. Each one cites its source in `source.reference`. They run against the Spring Boot app on every build, and also against a running Mule 3.8 app when `-Dparity.mule.base-url=...` is set (see [`parity-test-support`](../parity-test-support/README.md)).

## Behavioural deltas

- CXF is replaced by Spring WS. No WSDL is published at `?wsdl`, and SOAP faults for malformed requests use Spring WS's fault format and wording instead of CXF's.
- The CSV layout (comma separator, LF line endings, a trailing newline, quoting only when a value contains a separator, quote or newline) follows DataWeave 1.0 CSV writer defaults. It is derived from the documentation and has not been captured from a running Mule.
