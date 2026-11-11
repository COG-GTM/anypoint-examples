# REST API with APIkit (La Liga) — Spring Boot

Spring Boot 3 migration of [`rest-api-with-apikit`](../../rest-api-with-apikit). Hand-written `@RestController`s implement every resource in `leagues.raml` (`/api/teams`, `/api/teams/{teamId}`, `/api/positions`, `/api/fixture`, `/api/fixture/{homeTeamId}/{awayTeamId}`), with springdoc OpenAPI and a port of the in-memory `LeagueAPI`.

## Run

```bash
cd spring-boot-migration
mvn -B -pl rest-api-with-apikit -am verify          # build + parity tests
mvn -pl rest-api-with-apikit spring-boot:run        # or: java -jar rest-api-with-apikit/target/rest-api-with-apikit-1.0.0-SNAPSHOT.jar
```

The port defaults to `8080` and honours the Mule property: `-Dhttp.port=9090`.

```bash
curl http://localhost:8080/api/teams/BAR
curl -X PUT -H 'Content-Type: application/json' -d '{"homeTeamScore":3,"awayTeamScore":0}' http://localhost:8080/api/fixture/RMA/BAR
```

OpenAPI: `http://localhost:8080/api/v3/api-docs`, UI: `http://localhost:8080/api/swagger-ui.html`.

## Parity tests

`LeagueParityTest` replays the 24 stateful fixtures in `src/test/resources/parity/rest-api-with-apikit/` in file-name order against one league. They cover the MUnit `GET /api/teams/BAR`, team listing and city filter, create/update/delete with 201 + `Location` and 204, match read/update, and each APIkit mapping (400/404/405/406/415 with the exact `{ "message": ... }` bodies). They also cover cascades: deleting ZAR reverts BAR's played-match count to 18 and removes ZAR's matches. `RMA:BAR` is used because it is a played (first-round) match in Mule's `HashMap` iteration order, which this port keeps. `LeagueInvariantsTest` checks the random parts that can't be fixtures: 380 matches with 190 scored and dates at 18:00, and the position table ordering and points arithmetic.

Run against a live Mule runtime as well: `mvn -pl rest-api-with-apikit -am verify -Dparity.mule.base-url=http://localhost:8080`.

## Behavioural deltas

- **409 Conflict.** `POST /api/teams` with an existing id, and `PUT /api/fixture/{home}/{away}` on a match that hasn't been played, throw `ConflictException`. The RAML declares 409 and the Mule README says a 409 strategy exists, but `leagues-apiKitGlobalExceptionMapping` in the XML has **no** 409 mapping. In Mule that exception therefore falls through to the default strategy, which most likely returns a 500. Spring returns the RAML-declared **409 with an empty body**. No parity fixture covers this case because the two sides disagree. Please record the real Mule status with `-Dparity.mule.base-url` if it matters.
- **`Location` port.** Mule resolves `${http.port}` from the configuration. Spring uses the port it is actually listening on (`local.server.port`), which is the same value unless the server runs on a random port.
- **Paths outside `/api`.** Mule's listener only covers `/api/*`, so other paths get Mule's "no listener" 404. Spring (with context path `/api`) returns Tomcat's 404. Both are 404, but the bodies differ.
- **API console.** The APIkit console (`/api/console`) is replaced by springdoc: `/api/v3/api-docs` and `/api/swagger-ui.html`. The original RAML is still on the classpath as `leagues.raml`.
- **Validation messages.** RAML/JSON-schema validation is hand-written (`RamlValidator`) and always answers `{ "message": "Bad request" }`, as the Mule mapping does. When a request breaks several rules at once, the rule that gets reported may differ from APIkit's evaluation order.
- **JSON serialisation assumption.** The fixtures assume Mule renders the response POJOs with Mule's Jackson-1 `ObjectToJson`, which honours the codehaus `NON_NULL` and `JsonDateSerializer` annotations (`yyyy-MM-dd'T'HH:mm:ssZ`). Spring reproduces that output. `date` and the random first-round scores are excluded from the fixtures (`ignorePaths`) because they depend on the clock and on `Random`.

Mule ↔ Spring mapping: [MIGRATION-NOTES.md](MIGRATION-NOTES.md).
