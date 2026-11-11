# Exposing a RESTful Resource using the HTTP Connector — Spring Boot

Spring Boot 3 migration of [`exposing-a-restful-resource-using-the-HTTP-connector`](../../exposing-a-restful-resource-using-the-HTTP-connector). An in-memory `person` resource: `GET /person`, `POST /person` (201 + `Location`) and `GET /person/{personId}`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl exposing-a-restful-resource-using-the-HTTP-connector -am verify          # build + parity tests
mvn -pl exposing-a-restful-resource-using-the-HTTP-connector spring-boot:run        # or: java -jar exposing-a-restful-resource-using-the-HTTP-connector/target/exposing-a-restful-resource-using-the-HTTP-connector-1.0.0-SNAPSHOT.jar
```

The port defaults to `8081` and honours the Mule property: `-Dhttp.port=9090`.

```bash
curl -X POST -H 'Content-Type: application/json' -d @person.json http://localhost:8081/person
curl http://localhost:8081/person/1
```

OpenAPI: `http://localhost:8081/v3/api-docs`, UI: `/swagger-ui.html`.

## Parity tests

`RestfulResourceParityTest` replays the 8 stateful fixtures in `src/test/resources/parity/exposing-a-restful-resource-using-the-HTTP-connector/` in file-name order against one store (empty list, create → 201 + `Location: http://0.0.0.0:8081/person/1`, get, second create with a string `age`, list, missing id → 404 with the documented filter message, non-numeric id → 500, PUT → 405).

Run against a live Mule runtime as well: `mvn -pl exposing-a-restful-resource-using-the-HTTP-connector -am verify -Dparity.mule.base-url=http://localhost:8081`.

## Behavioural deltas

- **HTTP reason phrase.** Mule sets a custom reason phrase (`Not Found` on the 404 from `http:error-response-builder`). Tomcat (Spring Boot's embedded server) does not send reason phrases, so the status line is just `404`. Status codes are identical; clients that read the reason phrase see a difference.
- **`Content-Type` of the 201 response.** Mule returns the literal success JSON string from `set-payload` with no declared MIME type. Spring labels it `application/json`. The body is byte-for-byte the same, and the fixtures don't assert this header.
- **Body of the 500 for a non-numeric id.** Mule's default exception strategy writes the exception message as the body. Spring returns an empty body with the same status, 500.
- **Added springdoc endpoints.** `/v3/api-docs` and `/swagger-ui.html` are new. Mule answers 404 on those paths.

Mule ↔ Spring mapping: [MIGRATION-NOTES.md](MIGRATION-NOTES.md).
