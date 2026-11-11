# Testing APIkit with MUnit: Spring Boot migration

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`testing-apikit-with-munit`](../../testing-apikit-with-munit).

Implements `api.raml` under `/api`: `GET`/`DELETE /api/munit` → 200, `POST`/`PUT /api/munit` → 201, each with a fixed text body. APIkit's exception mapping is reproduced with a `@RestControllerAdvice`.

See [MIGRATION-NOTES.md](MIGRATION-NOTES.md) for the Mule construct → Spring mapping.

## Run

```bash
cd spring-boot-migration/testing-apikit-with-munit
mvn spring-boot:run
```

```bash
curl -i http://localhost:8081/api/munit
```

## Tests

```bash
cd spring-boot-migration
mvn -pl testing-apikit-with-munit -am verify
```

The parity fixtures under `src/test/resources/parity/testing-apikit-with-munit/` describe the Mule app's behaviour. Each one cites its source in `source.reference`. They run against the Spring Boot app on every build, and also against a running Mule 3.8 app when `-Dparity.mule.base-url=...` is set (see [`parity-test-support`](../parity-test-support/README.md)).

## Behavioural deltas

- The APIkit console (`/console`) is replaced by springdoc: the OpenAPI document is served at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`. The OpenAPI document is generated from the controllers, not converted from the RAML.
- Requests are not validated against the RAML at runtime. The RAML declares no request bodies, query parameters or headers, so the only validations APIkit could apply (resource, method, `Accept`) are covered by the controller mappings. The 400 mapping has no Spring trigger, and nothing in the RAML can produce a 415.
