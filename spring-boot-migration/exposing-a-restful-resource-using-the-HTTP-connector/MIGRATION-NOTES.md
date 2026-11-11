# Migration notes: `exposing-a-restful-resource-using-the-HTTP-connector`

- Original Mule app: [`../../exposing-a-restful-resource-using-the-HTTP-connector`](../../exposing-a-restful-resource-using-the-HTTP-connector)
- Mule flows: `http-restful-resource.xml` → `retrievePersonsFlow`, `storePersonFlow`, `retrievePersonFlow`
- Spring Boot module: `spring-boot-migration/exposing-a-restful-resource-using-the-HTTP-connector`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `spring:bean personDataStore` (HashMap) + `personId` (AtomicInteger) | `PersonStore` (`ConcurrentSkipListMap` + `AtomicInteger`) | Ordered map, so lists come back in id order, which is how a HashMap of small Integer keys iterates |
| `http:listener allowedMethods="GET" path="/person"` + `json:object-to-json-transformer` (`retrievePersonsFlow`) | `PersonController#all` (`@GetMapping("/person")`), Jackson |  |
| `http:listener allowedMethods="POST" path="/person"` + `http:response-builder statusCode=201` + `Location` header (`storePersonFlow`) | `PersonController#create` (`@PostMapping`, `ResponseEntity.status(201).header("Location", ...)`) | `Location` stays hard-coded to `http://0.0.0.0:8081/person/<id>`, as in the Mule XML |
| `json:json-to-object-transformer returnClass=org.mule.examples.Person` | `@RequestBody byte[]` + `ObjectMapper.readValue(body, Person.class)` (any Content-Type; `int age` coerces `"12"`) |  |
| `set-payload` success JSON string | `PersonController.CREATED_BODY` | Same literal text |
| `http:listener path="/person/{personId}"` + `Integer.valueOf(...)` + `logger level=ERROR` (`retrievePersonFlow`) | `PersonController#one` (`@GetMapping("/person/{personId}")`), `log.error("Person id is {}")` |  |
| `message-filter throwOnUnaccepted` + `rollback-exception-strategy` (404 / `Not Found`) | `Optional.orElseGet(() -> 404 + filter message)` | Body is the README-documented filter message |
| `choice-exception-strategy` with no matching branch | Explicit 500 on `NumberFormatException` |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| 01-list-empty | Empty store → `[]` | documented |
| 02-create-first | 201, literal body, `Location .../person/1` | munit |
| 03-get-first | GET /person/1 equals `person.json` | munit |
| 04-create-second-string-age | Id 2, string age coerced | documented |
| 05-list-all | List of both in id order | munit |
| 06-get-missing-404 | 404 + filter-rejected message | documented |
| 07-get-non-numeric-500 | Non-numeric id → 500 | documented |
| 08-put-not-allowed | PUT → 405 | documented |
| 09-create-without-content-type | `text/plain` body still parsed as JSON → 201 | documented |
| 10-get-created-without-content-type | That person is stored | documented |
| 11-create-invalid-json-500 | Invalid JSON → 500 | documented |
| 12-13 *-utf8-* | A UTF-8 body sent as `text/plain` with no charset round-trips unchanged | documented |

## Behavioural deltas

- **HTTP reason phrase.** Mule sets a custom reason phrase (`Not Found` on the 404 from `http:error-response-builder`). Tomcat (Spring Boot's embedded server) does not send reason phrases, so the status line is just `404`. Status codes are identical; clients that read the reason phrase see a difference.
- **`Content-Type` of the 201 response.** Mule returns the literal success JSON string from `set-payload` with no declared MIME type. Spring labels it `application/json`. The body is byte-for-byte the same, and the fixtures don't assert this header.
- **Body of the 500s (non-numeric id, invalid JSON).** Mule's default exception strategy writes the exception message as the body. Spring returns an empty body with the same status, 500.
- **Added springdoc endpoints.** `/v3/api-docs` and `/swagger-ui.html` are new. Mule answers 404 on those paths.
