# addition-using-javascript-transformer (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`addition-using-javascript-transformer`](../../addition-using-javascript-transformer).

POST a JSON object to `/`; the values are summed and returned as `Sum is: <sum>.`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl addition-using-javascript-transformer -am verify          # build + parity tests
mvn -pl addition-using-javascript-transformer spring-boot:run        # then: curl -X POST localhost:8081/ -d '{"a":1,"b":2}'
```

## Tests

- Parity fixtures: `src/test/resources/parity/addition-using-javascript-transformer/` (5 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `AdditionUsingJavascriptTransformerParityTest`, `JavaScriptSumTableTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The Rhino script engine is gone. `JavaScriptSum` emulates the parts of JavaScript the script uses (number addition, string concatenation, `null`/boolean coercion, arrays). Inputs that relied on other JavaScript `eval` features (functions, expressions inside the payload) are rejected as invalid JSON instead of being evaluated.
