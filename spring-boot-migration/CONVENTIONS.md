# Conventions for migrated modules

## Module layout

```
spring-boot-migration/<mule-app-name>/
├── pom.xml                      # parent = org.mule.examples.springboot:spring-boot-migration
├── README.md                    # what the app does, how to run it, behavioural deltas vs Mule
├── MIGRATION-NOTES.md           # copied from ../MIGRATION-NOTES.template.md
└── src
    ├── main/java/org/mule/examples/springboot/<package>/...
    ├── main/resources/application.yml
    ├── test/java/org/mule/examples/springboot/<package>/...ParityTest.java
    └── test/resources/
        ├── application-parity.yml
        └── parity/<mule-app-name>/*.json   # one fixture per case (+ optional bodies/ dir)
```

- Directory name and `artifactId` are the Mule app's directory name, unchanged (e.g. `rest-api-with-apikit`).
- `groupId` is inherited (`org.mule.examples.springboot`); version is inherited (`1.0.0-SNAPSHOT`).
- Java package: `org.mule.examples.springboot.<app name, lower-case, non-alphanumerics removed>`, e.g.
  `org.mule.examples.springboot.restapiwithapikit`.
- One `@SpringBootApplication` per module. Java 17 language level (records, switch expressions and text blocks are fine).
- No module depends on another migrated module. Shared code goes in `parity-test-support` (test-only) or is duplicated.

Minimal module `pom.xml`:

```xml
<parent>
    <groupId>org.mule.examples.springboot</groupId>
    <artifactId>spring-boot-migration</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>hello-world</artifactId>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mule.examples.springboot</groupId>
        <artifactId>parity-test-support</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

## `application.yml`

- Keep the Mule app's externally visible settings, and keep their **property names** where possible so existing
  `mule-app.properties` / `mule-artifact.properties` values carry over:
  - HTTP port: `server.port: ${http.port:8081}` (Mule examples listen on `${http.port}`, documented as 8081).
  - Listener base path: if the Mule `http:listener-config` had a `basePath`, use `server.servlet.context-path`.
  - Connector credentials (Salesforce, NetSuite, SAP, DB, JMS...): keep the Mule key, e.g.
    `sfdc.username: ${sfdc.username:}`; never commit real credentials. Defaults are blank or point at localhost.
- Downstream base URLs are always properties (e.g. `salesforce.base-url`) so tests can point them at WireMock.
- `src/test/resources/application-parity.yml` maps those properties to `${parity.downstream.base-url}` (WireMock) or to
  Testcontainers. The `parity` profile is active in every `AbstractParityTest`, and `parity/parity-defaults.yml` from
  `parity-test-support` is imported automatically.
- Logging: Mule `logger` processors become SLF4J logs at the same level and with the same message.

## Mapping Mule constructs

| Mule 3.8 | Spring Boot 3 |
|---|---|
| `http:listener` + flow | `@RestController` method (or Spring Integration `Http.inboundGateway` when the flow is a real pipeline) |
| APIkit router + RAML | `@RestController`s matching every RAML resource/method; springdoc for the OpenAPI document |
| DataWeave / `json-to-object` / `object-to-json` | Jackson + plain Java mappers or MapStruct |
| MEL expressions | Java code, or SpEL where an expression is configured |
| `choice` / `foreach` / `scatter-gather` / `filter` | Spring Integration router / splitter / aggregator / filter, or plain Java |
| `flow-ref` / sub-flow | method call or Spring Integration channel |
| `catch` / `choice-exception-strategy` | `@ControllerAdvice` / Spring Integration error channel |
| `ee:cache` | Spring Cache (`@Cacheable`) |
| JMS / AMQP connectors | `JmsTemplate` + `@JmsListener`, Spring AMQP |
| `transactional` scope | `@Transactional` (JMS + JDBC via a chained or JTA transaction manager) |
| Batch module / watermark | Spring Batch; watermark persisted in a table |
| SaaS connectors | Typed `RestClient`/`WebClient` service clients |
| File / FTP / SFTP / IMAP / POP3 / SMTP | Spring Integration adapters, Spring Mail |
| MUnit | JUnit 5 + parity tests |

Every module records the constructs it actually used in its `MIGRATION-NOTES.md`.

## Parity fixtures

A fixture is one JSON file per case under `src/test/resources/parity/<mule-app-name>/`. Full schema:
[`parity-test-support/README.md`](parity-test-support/README.md).

- `source.evidence` says where the expected values come from: `documented` (read from the flow XML, DataWeave,
  RAML examples or README), `recorded` (captured from a running Mule 3.8 with `-Dparity.record=true`) or `munit` (copied
  from an MUnit assertion). Always put the file and element you read in `source.reference`.
- Expected values are what the **Mule** app returns or does. When the Spring Boot module intentionally differs, keep
  the Mule value in the fixture and document the delta. Never edit a fixture to match the new code.
- For non-HTTP apps (JMS, AMQP, file, email, batch), use the same fixture files as the source of input/expected data
  and assert with the comparators in `ParityComparator` (`compareBody`), so JSON/XML equivalence works the same way.

## Definition of done (per ticket)

1. `mvn verify` from `spring-boot-migration/` is green, including the parity tests.
2. Each module has `README.md` (run instructions + **Behavioural deltas** section, "None" if none) and
   `MIGRATION-NOTES.md`.
3. The app's row in the top-level README table is updated.
