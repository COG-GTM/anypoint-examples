# mule-expression-language-basics (Spring Boot)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`mule-expression-language-basics`](../../mule-expression-language-basics).

Six `/greetN` endpoints show MEL basics: query parameters, conditionals, writing files, and reading XML or JSON bodies.

## Run

```bash
cd spring-boot-migration
mvn -B -pl mule-expression-language-basics -am verify          # build + parity tests
mvn -pl mule-expression-language-basics spring-boot:run        # then: GET http://localhost:8081/greet1?username=Mule
```

## Tests

- Parity fixtures: `src/test/resources/parity/mule-expression-language-basics/` (9 cases). Each one cites its Mule source in `source.reference`.
- Test classes: `GreetingExpressionsTableTest`, `MuleExpressionLanguageBasicsParityTest`.
- To check against a running Mule app: `-Dparity.mule.base-url=http://host:port`.

See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- Output file names are `<uuid>.dat`. Mule's file connector made up its own names, and the contents are the same.
- The output directory can be set with `mel.output-dir`.
