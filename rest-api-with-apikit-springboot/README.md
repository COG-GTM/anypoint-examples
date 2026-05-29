# REST API with APIkit — Spring Boot migration

This is a **Spring Boot 3 / Java 21** migration of the MuleSoft
[`rest-api-with-apikit`](../rest-api-with-apikit) example (the "La Liga"
Leagues API). It is a self-contained, runnable application that exposes the
same REST contract the original Mule app served via a RAML + APIkit router,
re-implemented with Spring MVC.

The original Mule project is left untouched alongside this one so the two can
be compared side by side.

## What the API does

Add, remove, and retrieve information about teams participating in a league
tournament, view the standings table, and view/update the fixture (matches).
The league is seeded in memory from [`teams.json`](src/main/resources/teams.json)
at startup, and a fixture (home & away matches for every pair of teams) is
generated, with first-round matches given random results.

## Endpoints

Base path: `/api`

| Method | Path | Description |
| ------ | ---- | ----------- |
| GET | `/api/teams` | List teams (optional `?city=` filter) |
| POST | `/api/teams` | Add a team (201 + `Location`; 409 if it exists) |
| GET | `/api/teams/{teamId}` | Get a team incl. matches played (404 if missing) |
| PUT | `/api/teams/{teamId}` | Partially update a team (204; 404 if missing) |
| DELETE | `/api/teams/{teamId}` | Remove a team (204; 404 if missing) |
| GET | `/api/positions` | Standings table |
| GET | `/api/fixture` | All matches |
| GET | `/api/fixture/{homeTeamId}/{awayTeamId}` | A single match (404 if missing) |
| PUT | `/api/fixture/{homeTeamId}/{awayTeamId}` | Update a match score (204; 404; 409 if not played yet) |

Interactive docs (the modern equivalent of the APIkit console) are served by
Swagger UI at `http://localhost:8080/swagger-ui.html`, with the OpenAPI
document at `http://localhost:8080/v3/api-docs`.

## How the MuleSoft concepts map to Spring Boot

| MuleSoft (`rest-api-with-apikit`) | Spring Boot (this project) |
| --------------------------------- | -------------------------- |
| `leagues.raml` + `apikit:config` router + per-resource `<flow>`s | `@RestController`s (`TeamsController`, `FixtureController`, `PositionsController`) |
| `http:listener` on `/api/*` + Mule runtime | Spring MVC + embedded Tomcat (`LeaguesApplication`) |
| `LeagueAPI` Spring bean (`init-method="initialize"`) wired into flows | `LeagueService` (`@Service`, `@PostConstruct` initializer) |
| `apikit:mapping-exception-strategy` (404/405/415/406/400 → JSON) | `@RestControllerAdvice` (`GlobalExceptionHandler`) |
| APIkit `NotFoundException` / example `ConflictException` | `NotFoundException` / `ConflictException` (mapped to 404 / 409) |
| `request.*` / `response.*` POJOs | `dto.*` request classes and response records |
| `model.League` / `Team` / `Match` | ported as-is to `model.*` (JSON parsing switched to Jackson, logging to SLF4J) |
| `JsonDateSerializer` (`yyyy-MM-dd'T'HH:mm:ssZ`) | `@JsonFormat` on `MatchResponse.date` |
| RAML required fields | Bean Validation (`@NotBlank`) on `TeamRequest` |
| APIkit console | Swagger UI (springdoc-openapi) |

The domain logic in `model/` (fixture generation, standings ordering, applying
and reverting match results) is preserved unchanged.

## Build and run

Requires JDK 21.

```bash
# run the tests
mvn test

# run the app (http://localhost:8080)
mvn spring-boot:run

# or build a jar and run it
mvn package
java -jar target/rest-api-with-apikit-springboot-1.0.0.jar
```

### Try it

```bash
curl http://localhost:8080/api/teams/BAR
curl "http://localhost:8080/api/teams?city=Madrid"
curl http://localhost:8080/api/positions
curl -X POST http://localhost:8080/api/teams \
  -H 'Content-Type: application/json' \
  -d '{"id":"TST","name":"Test FC","homeCity":"Testville","stadium":"Test Arena"}'
```

## Tests

`mvn test` runs an end-to-end MockMvc suite (`LeagueApiIntegrationTest`) that
exercises the success and error paths (200/201/204/400/404/405/409) against a
freshly initialized league, plus a context-load smoke test.
