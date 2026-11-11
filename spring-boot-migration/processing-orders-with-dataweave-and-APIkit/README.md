# Processing Orders with DataWeave and APIkit — Spring Boot

Spring Boot 3 migration of [`processing-orders-with-dataweave-and-APIkit`](../../processing-orders-with-dataweave-and-APIkit). A Spring Integration file poller picks up `orders.xml` files from `src/main/resources/input`. It fetches currency ratios from the app's own `GET /api/currencies` (the APIkit currency API), maps the orders in Java instead of DataWeave, and writes `orders.json` and `report.csv` to `src/main/resources/output`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl processing-orders-with-dataweave-and-APIkit -am verify          # build + parity tests
mvn -pl processing-orders-with-dataweave-and-APIkit spring-boot:run        # or: java -jar processing-orders-with-dataweave-and-APIkit/target/processing-orders-with-dataweave-and-APIkit-1.0.0-SNAPSHOT.jar
```

The port defaults to `8081` and honours the Mule property: `-Dhttp.port=9090`.

`orders.input-dir` and `orders.output-dir` default to the Mule paths `src/main/resources/input` and `src/main/resources/output`. As in Mule, these relative paths resolve against the **working directory**, so start the app from the module directory or pass absolute paths:

```bash
cd spring-boot-migration/processing-orders-with-dataweave-and-APIkit
java -jar target/processing-orders-with-dataweave-and-APIkit-1.0.0-SNAPSHOT.jar
# in another shell, from the same directory:
cp ../../processing-orders-with-dataweave-and-APIkit/src/main/resources/orders.xml src/main/resources/input/
cat src/main/resources/output/orders.json src/main/resources/output/report.csv
curl http://localhost:8081/api/currencies

# or from anywhere:
java -jar .../processing-orders-with-dataweave-and-APIkit-1.0.0-SNAPSHOT.jar --orders.input-dir=/abs/in --orders.output-dir=/abs/out
```

Directories: `orders.input-dir`, `orders.output-dir`. Currency API override: `orders.currency-base-url`.

## Parity tests

`CurrencyApiParityTest` replays the 3 fixtures in `src/test/resources/parity/processing-orders-with-dataweave-and-APIkit/` (`GET /api/currencies` returns `currency.json`, unknown resource → 404, POST → 405, with the APIkit mapping bodies). `OrderFlowParityTest` repeats the Mule IT `ProcessingOrdersWithDataweaveAndApikitIT`: it drops the Mule `orders.xml` into the input directory and compares `orders.json` and `report.csv` with Mule's expected files (copied verbatim to `order-flow/`). It also checks that the input file is consumed. The currency lookup goes over real HTTP to the app's own `/api/currencies`, as in Mule. `OrderMapperTest` unit-tests the DataWeave replacement (year filter, `distinctBy`, report arithmetic).

Run against a live Mule runtime as well: `mvn -pl processing-orders-with-dataweave-and-APIkit -am verify -Dparity.mule.base-url=http://localhost:8081`.

## Behavioural deltas

- **`orders.json` whitespace.** Spring writes Jackson pretty-printed JSON, so the whitespace differs from DataWeave's output. Keys, order and number text (`27.60`, `262.7124`, ...) are the same. The Mule IT and the parity test compare with whitespace stripped.
- **Hidden files are ignored.** The inbound adapter skips dot-files. This keeps the `.gitkeep` in `src/main/resources/input` from being treated as an order file. Mule would try to process any file it finds.
- **Failure handling.** If a file can't be parsed or the currency call fails, Spring logs the error and leaves the file in place. Mule's default strategy also logs, and does not write output.
- **Paths outside `/api`.** Requests there get Spring's 404 handling instead of Mule's "no listener" response. The APIkit console is replaced by springdoc (`/v3/api-docs`, `/swagger-ui.html`).

Mule ↔ Spring mapping: [MIGRATION-NOTES.md](MIGRATION-NOTES.md).
