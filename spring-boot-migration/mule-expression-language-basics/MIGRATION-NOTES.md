# Migration notes: `mule-expression-language-basics`

- Original Mule app: [`../../mule-expression-language-basics`](../../mule-expression-language-basics)
- Mule flows: `mule-expression-language-basics.xml` → `greet1Flow`..`greet6Flow`
- Spring Boot module: `spring-boot-migration/mule-expression-language-basics`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/greet1"`..`/greet6` | `GreetingController` |  |
| MEL expressions (`#['Hello ' + message.inboundProperties...]`, `#[... == empty]`) | SpEL expressions in `GreetingExpressions` (`SimpleEvaluationContext`) | MEL → SpEL as the ticket asks. |
| `file:outbound-endpoint` | `OutputFileWriter` / `CsvWriter` writing to `mel.output-dir` |  |
| `xpath3` / JSON path reads | DOM/Jackson reads feeding SpEL variables |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `mel.output.dir` | `mel.output-dir` (`${mel.output.dir:...}`) | `Path_of_your_choice` (as in Mule) |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-greet1-query-param.json` | Example 1: set-payload 'Hello #[...http.query.params.username]'. | `munit` |
| `02-greet2-username-present.json` | Example 2: choice otherwise branch when username is not empty. | `munit` |
| `03-greet2-username-missing.json` | Example 2: username == empty (absent) selects 'No username provided'. | `munit` |
| `04-greet2-username-blank.json` | MEL empty also matches an empty string. | `documented` |
| `05-greet3-writes-summary-file.json` | Example 3: writes 'username, age, age > 18' to the output folder and replies with the greeting. | `munit` |
| `06-greet3-adult.json` | age > 18 coerces the query String to a number: 30 > 18 is true. | `documented` |
| `07-greet4-writes-csv-file.json` | Example 4: map -> DataWeave CSV without header -> file. | `munit` |
| `08-greet5-xpath.json` | Example 5: XPath /user/username and /user/age from the XML body. | `munit` |
| `09-greet6-json.json` | Example 6: JSON -> Object, payload.username / payload.age. | `munit` |

Other tests: `GreetingExpressionsTableTest`, `MuleExpressionLanguageBasicsParityTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

- Output file names are `<uuid>.dat`. Mule's file connector made up its own names, and the contents are the same.
- The output directory can be set with `mel.output-dir`.
