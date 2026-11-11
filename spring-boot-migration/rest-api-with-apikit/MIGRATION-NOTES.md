# Migration notes: `rest-api-with-apikit`

- Original Mule app: [`../../rest-api-with-apikit`](../../rest-api-with-apikit)
- Mule flows: `rest-api-with-apikit.xml` → `main` + the APIkit flows `get:/teams`, `post:/teams`, `get|put|delete:/teams/{teamId}`, `get:/positions`, `get:/fixture`, `get|put:/fixture/{homeTeamId}/{awayTeamId}`
- Spring Boot module: `spring-boot-migration/rest-api-with-apikit`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/api/*"` + `apikit:router config=leagues-config raml=leagues.raml` (`main`) | `server.servlet.context-path: /api` + `LeagueController` (`@RestController`) | Hand-written from the RAML, documented with springdoc `@Operation` |
| `get:/teams:leagues-config` (`getTeams(http.query.params.city)`) | `LeagueController#getTeams` (`@GetMapping("/teams") @RequestParam city`) |  |
| `post:/teams:leagues-config` + `set-property Location=http://localhost:${http.port}/api/teams/#[payload.id]` + NullPayload | `LeagueController#addTeam` → `ResponseEntity.created(...)` | 201 is the RAML success code |
| `get|put|delete:/teams/{teamId}:leagues-config` | `LeagueController#getTeam/updateTeam/deleteTeam` | PUT/DELETE → 204 (RAML) |
| `get:/positions`, `get:/fixture`, `get|put:/fixture/{homeTeamId}/{awayTeamId}` | `LeagueController#getPositions/getFixture/getMatch/updateScore` |  |
| `PayloadNormalizerTransformer` + RAML JSON schemas (draft-03) + uriParameters min/maxLength 3 | `RamlValidator` (Jackson `JsonNode` checks), `consumes`/`produces` = `application/json` |  |
| `apikit:mapping-exception-strategy leagues-apiKitGlobalExceptionMapping` (404/405/415/406/400) | `ApiKitExceptionMapping` (`@RestControllerAdvice`) | Same status codes, `Content-Type: application/json` and bodies |
| `spring:bean league-api` `org.mule.examples.leagues.LeagueAPI` (init-method `initialize`) | `LeagueService` + `model.League/Team/Match` | Same `HashMap` storage and fixture-building algorithm |
| `response.*` POJOs with codehaus `@JsonSerialize(NON_NULL)` / `JsonDateSerializer` | `ApiResponses` records with `@JsonInclude(NON_NULL)` / `IsoDateSerializer` |  |
| `apikit:config consoleEnabled` (`/api/console`) | springdoc (`/api/v3/api-docs`, `/api/swagger-ui.html`) |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` (`mule-app.properties`: 8080) | `server.port` (`${http.port:8080}`) | 8080 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| 01, 11, 23 get-team-* | Team lookup incl. MUnit `reply.json`, update and delete cascade | munit / documented |
| 02-03 list-teams* | All teams / city filter (order-insensitive: HashMap order) | documented |
| 04, 15, 20, 21, 24 *-404 | NotFound mapping | documented |
| 05, 08, 17 *-400 | URI-param / schema validation | documented |
| 09 *-415 | Unsupported media type | documented |
| 10 *-406 | Not acceptable | documented |
| 18 post-positions-405 | Method not allowed | documented |
| 19 unknown-resource-404 | Resource not in RAML | documented |
| 06-07 create-team / get-created-team | 201 + Location, then visible | documented |
| 12-14 match | First-round match read, score update 204, updated score | documented |
| 25-26 partial score | Omitted score stored as 0 (primitive `int` in `Score`) | documented |
| 27-28 *-null-400 | Explicit JSON null fails the schema `type` → 400 | documented |
| `LeagueInvariantsTest` | Fixture/positions invariants for random data | documented |

## Behavioural deltas

- **409 Conflict.** `POST /api/teams` with an existing id, and `PUT /api/fixture/{home}/{away}` on a match that hasn't been played, throw `ConflictException`. The RAML declares 409 and the Mule README says a 409 strategy exists, but `leagues-apiKitGlobalExceptionMapping` in the XML has **no** 409 mapping. In Mule that exception therefore falls through to the default strategy, which most likely returns a 500. Spring returns the RAML-declared **409 with an empty body**. No parity fixture covers this case because the two sides disagree. Please record the real Mule status with `-Dparity.mule.base-url` if it matters.
- **`Location` port.** Mule resolves `${http.port}` from the configuration. Spring uses the port it is actually listening on (`local.server.port`), which is the same value unless the server runs on a random port.
- **Paths outside `/api`.** Mule's listener only covers `/api/*`, so other paths get Mule's "no listener" 404. Spring (with context path `/api`) returns Tomcat's 404. Both are 404, but the bodies differ.
- **API console.** The APIkit console (`/api/console`) is replaced by springdoc: `/api/v3/api-docs` and `/api/swagger-ui.html`. The original RAML is still on the classpath as `leagues.raml`.
- **Validation messages.** RAML/JSON-schema validation is hand-written (`RamlValidator`) and always answers `{ "message": "Bad request" }`, as the Mule mapping does. When a request breaks several rules at once, the rule that gets reported may differ from APIkit's evaluation order.
- **JSON serialisation assumption.** The fixtures assume Mule renders the response POJOs with Mule's Jackson-1 `ObjectToJson`, which honours the codehaus `NON_NULL` and `JsonDateSerializer` annotations (`yyyy-MM-dd'T'HH:mm:ssZ`). Spring reproduces that output. `date` and the random first-round scores are excluded from the fixtures (`ignorePaths`) because they depend on the clock and on `Random`.
