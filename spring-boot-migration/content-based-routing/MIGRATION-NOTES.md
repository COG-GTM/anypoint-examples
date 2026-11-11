# Migration notes: `content-based-routing`

- Original Mule app: [`../../content-based-routing`](../../content-based-routing)
- Mule flows: `content-based-routing.xml` → `content-based-routingFlow`
- Spring Boot module: `spring-boot-migration/content-based-routing`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/"` | `ContentBasedRoutingController` (`/`) | Favicon requests get an empty 200 like the Mule filter. |
| `set-variable language = #[message.inboundProperties.'http.query.params'.language]` | `GreetingRequest` record | MEL replaced by a request parameter. |
| `choice` (`#[flowVars.language == 'Spanish']`, `'French'`, otherwise) | Spring Integration `route()` with `subFlowMapping` in `ContentBasedRoutingFlowConfig`; table in `LanguageRoutes` | Case-sensitive, as in MEL `==`. |
| `set-payload` / `set-variable language=English` in the default branch | sub-flow `handle` that sets the `language` header |  |
| `logger` | SLF4J log in the final `handle` | Same message text. |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-spanish.json` | language=Spanish routes to the 'Reply in Spanish' branch. | `munit` |
| `02-french.json` | language=French routes to the 'Reply in French' branch. | `munit` |
| `03-no-language-defaults-to-english.json` | No language parameter: otherwise branch, replyInDefaultLanguage sub-flow replies in English. | `documented` |
| `04-unknown-language-defaults-to-english.json` | An unmapped language (German) also falls through to the otherwise branch. | `documented` |
| `05-language-match-is-case-sensitive.json` | MEL == is String.equals, so 'spanish' does not match 'Spanish' and gets the default reply. | `documented` |
| `06-favicon-payload-is-filtered.json` | expression-filter #[payload != '/favicon.ico'] drops the message; the listener answers 200 with an empty body. | `documented` |

Other tests: `ContentBasedRoutingParityTest`, `ContentBasedRoutingTableTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

None.
