# Migration notes: `testing-apikit-with-munit`

- Original Mule app: [`../../testing-apikit-with-munit`](../../testing-apikit-with-munit)
- Mule flows: `api.xml` → `api-main`, `get:/munit:api-config`, `post:/munit:api-config`, `put:/munit:api-config`, `delete:/munit:api-config`
- Spring Boot module: `spring-boot-migration/testing-apikit-with-munit`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/api/*"` + `apikit:router config-ref="api-config"` (`api-main`) | `MunitResourceController` (`@RequestMapping("/api/munit")`), the RAML is kept as `src/main/resources/api.raml` | Routes are declared statically instead of being generated from the RAML at runtime |
| `get/post/put/delete:/munit:api-config` flows with `set-payload` | `MunitResourceController#get/post/put/delete` | 201 for POST/PUT via `@ResponseStatus(CREATED)`, as declared in the RAML |
| `apikit:mapping-exception-strategy` 404 / 405 / 415 / 406 / 400 | `ApiExceptionMapping` (`@RestControllerAdvice`) for `NoResourceFoundException` / `HttpRequestMethodNotSupportedException` / `HttpMediaTypeNotSupportedException` / `HttpMediaTypeNotAcceptableException` | Same JSON bodies, `Content-Type: application/json` |
| `apikit:console` (`api-console` flow) | springdoc OpenAPI document (`/v3/api-docs`) and Swagger UI (`/swagger-ui.html`) | Generated from the controllers; see behavioural deltas |
| MUnit `api-test-suite.xml` (`http:request` + `assert-on-equals http.status` + `assert-payload-equals`) | Parity fixtures `01`–`04` (`TestingApikitWithMunitParityTest`) |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-get-munit.json` | GET /api/munit returns 200 and 'GET RESPONSE'. | `munit` |
| `02-post-munit.json` | POST /api/munit returns 201 (RAML response code) and 'POST RESPONSE'. | `munit` |
| `03-put-munit.json` | PUT /api/munit returns 201 (RAML response code) and 'PUT RESPONSE'. | `munit` |
| `04-delete-munit.json` | DELETE /api/munit returns 200 and 'DELETE RESPONSE'. | `munit` |
| `05-unknown-resource.json` | A path not declared in the RAML raises NotFoundException, mapped to 404 with a JSON message. | `documented` |
| `06-method-not-allowed.json` | PATCH is not declared for /munit in the RAML, so APIkit raises MethodNotAllowedException, mapped to 405 with a JSON message. | `documented` |
| `07-not-acceptable.json` | The RAML only declares application/json responses, so Accept: application/xml raises NotAcceptableException, mapped to 406 with a JSON message. | `documented` |

## Behavioural deltas

- The APIkit console (`/console`) is replaced by springdoc: the OpenAPI document is served at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`. The OpenAPI document is generated from the controllers, not converted from the RAML.
- Requests are not validated against the RAML at runtime. The RAML declares no request bodies, query parameters or headers, so the only validations APIkit could apply (resource, method, `Accept`) are covered by the controller mappings. The 400 mapping has no Spring trigger, and nothing in the RAML can produce a 415.
