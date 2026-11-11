# parity-test-support

Shared, test-scoped harness used by every migrated module to prove that the Spring Boot app behaves like the original
Mule 3.8 flow.

```xml
<dependency>
    <groupId>org.mule.examples.springboot</groupId>
    <artifactId>parity-test-support</artifactId>
    <scope>test</scope>
</dependency>
```

## What's in it

| Class | Use |
|---|---|
| `ParityFixture` / `ParityFixtures` | Fixture model + loader for `classpath:<dir>/*.json` (resolves `bodyFile` references) |
| `AbstractParityTest` | `@SpringBootTest(RANDOM_PORT)`, profile `parity`; turns every fixture into a dynamic test (capture-and-compare) |
| `AbstractWireMockParityTest` | Same, plus a shared WireMock server for downstream systems (`parity.downstream.base-url`), per-case stubs and outbound-request verification |
| `ParityHttpClient` | Replays a fixture request against any base URL; never follows redirects |
| `ParityComparator` / `ParityAssertions` | Status, header (`exact`, `~regex`, `!absent`) and body comparison: JSON (JSONAssert, `ignorePaths`), XML (XMLUnit similarity), text (exact or regex) |
| `ParityRecorder` | Writes responses captured from a live Mule runtime as fixtures (`evidence: recorded`) |
| `WireMockParitySupport` | Stand-alone WireMock helper (stub downstream, verify outbound) for non-HTTP-entry modules |
| `ParityContainers` | Testcontainers factories: MySQL 5.7, ActiveMQ, RabbitMQ, MongoDB, GreenMail, SFTP |

## How a parity case runs

1. `beforeCase` (WireMock reset + fixture `downstream` stubs; override to seed a DB, etc.).
2. Request is sent to the Spring Boot app; response must match the fixture's `expected`.
3. `afterCase` (fixture `expectedOutbound` verified against WireMock; override for DB/queue side effects).
4. If `-Dparity.mule.base-url=http://host:port` is set, the same request goes to the running Mule app. Mule must match
   the fixture, **and** Mule and Spring Boot responses must be equivalent to each other.
5. With `-Dparity.record=true` as well, Mule's response is written to `target/parity-recordings/<case>.json`.

```bash
# fixtures only (CI default)
mvn -pl hello-world -am verify
# also compare against the original app running in Mule 3.8 / Anypoint Studio, and record its responses
mvn -pl hello-world -am verify -Dparity.mule.base-url=http://localhost:8081 -Dparity.record=true
```

## Fixture format

```json
{
  "name": "create-customer-forwards-to-crm",
  "description": "What this case proves",
  "source": { "muleApp": "hello-world", "flow": "HelloWorldFlow1", "evidence": "documented|recorded|munit",
              "reference": "hello-world/src/main/app/HelloWorld.xml" },
  "request":  { "method": "POST", "path": "/sample/customers",
                "headers": { "Content-Type": "application/json" },
                "body": { "firstName": "Ada" } },
  "downstream": [
    { "method": "POST", "urlPath": "/crm/customers", "queryParams": {}, "status": 200,
      "headers": { "Content-Type": "application/json" }, "body": { "id": "C-1001" } }
  ],
  "expectedOutbound": [
    { "method": "POST", "urlPath": "/crm/customers", "count": 1,
      "headers": { "Content-Type": "~application/json.*" },
      "bodyFormat": "JSON", "body": { "name": "Ada Lovelace" } }
  ],
  "expected": {
    "status": 201,
    "headers": { "Content-Type": "application/json", "X-Debug": "!absent" },
    "bodyFormat": "JSON",
    "body": { "customerId": "C-1001", "status": "CREATED" },
    "jsonCompareMode": "STRICT",
    "ignorePaths": ["**.timestamp"]
  }
}
```

- `body` is either a string (sent/compared verbatim, e.g. XML or CSV) or any JSON value. `bodyFile` (path relative to
  the fixture file) replaces `body`, which is handy for SOAP envelopes.
- `bodyFormat`: `JSON`, `XML`, `TEXT`, `NONE` (must be empty) or `IGNORE` (default).
- `jsonCompareMode`: a JSONAssert mode. `STRICT` (default) means no extra fields and array order matters; `LENIENT`,
  `NON_EXTENSIBLE` and `STRICT_ORDER` are also accepted. Only use `ignorePaths` for genuinely volatile values (generated
  ids, timestamps) and say why in `description`.

## Sample harness

`src/test/java/.../sample/` holds a minimal app and three fixtures (`hello-world` text, JSON with a downstream
side effect, XML) run by:

- `SampleParityTest`: fixtures vs Spring Boot, with WireMock downstream verification.
- `MuleCaptureModeParityTest`: replays the fixtures from a WireMock "fake Mule" on its own port as
  `parity.mule.base-url`, which exercises the Mule-vs-Spring comparison and the recorder without a Mule runtime.
- `ParityComparatorTest`: checks the comparators accept equivalent payloads and **reject** divergent ones.
- `ParityContainersTest`: starts MySQL 5.7 through `ParityContainers` (needs Docker).
