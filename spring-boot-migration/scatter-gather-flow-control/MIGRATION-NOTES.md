# Migration notes: `scatter-gather-flow-control`

- Original Mule app: [`../../scatter-gather-flow-control`](../../scatter-gather-flow-control)
- Mule flows: `scatter-gather-flow-control.xml` → `scatter-gather-flow-controlFlow`
- Spring Boot module: `spring-boot-migration/scatter-gather-flow-control`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/scatterGather"` | `ScatterGatherController` |  |
| `scatter-gather` with two routes (source A / source B) | Spring Integration `scatterGather()` with two `recipientFlow`s in `ScatterGatherFlowConfig`; data in `ContactSources` |  |
| `custom-aggregation-strategy` (`ContactMerge`) | `ContactMergeAggregationStrategy` → `ContactMerge` | The result is wrapped in `MergedContacts` so Spring Integration does not split the list. |
| DataWeave → CSV | `AggregationReportCsv` | Columns `Id,Name,Email`. |
| Gmail SMTP connector + attachment `aggregation-report` | `AggregationReportMailer` (`JavaMailSender`) |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| `smtp.host`/`smtp.port`/`smtp.user`/`smtp.password` | `spring.mail.*` (`${smtp.host:smtp.gmail.com}`, ...) | smtp.gmail.com:465, no credentials committed |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-scatter-gather-post.json` | POST /scatterGather: both routes run, ContactMergeAggregationStrategy merges A and B by e-mail, DataWeave writes the CSV (merged contacts have no Id key, so the Id column is empty), outboundFlow e-mails it as the aggregation-report attachment and the response is the payload set before the SMTP endpoint. | `munit` |
| `02-scatter-gather-get.json` | GET /scatterGather: both routes run, ContactMergeAggregationStrategy merges A and B by e-mail, DataWeave writes the CSV (merged contacts have no Id key, so the Id column is empty), outboundFlow e-mails it as the aggregation-report attachment and the response is the payload set before the SMTP endpoint. | `documented` |

Other tests: `ContactMergeTableTest`, `ScatterGatherFlowControlParityTest` (the table-driven tests send the same inputs through the ported Mule routing/transformation logic).

## Behavioural deltas

- The Gmail connector became plain SMTP (`spring.mail.*`). Parity tests use a GreenMail server instead of Gmail.
- Line endings in the attachment are compared after turning CRLF into LF, because SMTP sends CRLF.
