# Migration notes: `sending-a-csv-file-through-email-using-smtp`

- Original Mule app: [`../../sending-a-csv-file-through-email-using-smtp`](../../sending-a-csv-file-through-email-using-smtp)
- Mule flows: `csv-to-smtp.xml` → `csv-to-smtpFlow`
- Spring Boot module: `spring-boot-migration/sending-a-csv-file-through-email-using-smtp`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `file:inbound-endpoint` "Poll files" | `CsvToSmtpFlowConfiguration#csvToSmtpFlow`: `Files.inboundAdapter` + poller, file deleted after reading | Mule defaults: 1000 ms, `autoDelete=true` |
| `dw:transform-message` "Transform CSV to Map" (`as :number`) | `OrdersCsv#parse` | Integral numbers become `Integer` (`2.0` → `2`, `2.0*10` → `20`), others plain decimals, so `List#toString()` is identical to the documented `email.txt` |
| `logger` "Processing objects: #[payload]" | SLF4J, same message |  |
| `smtp:gmail-connector` + `smtp:outbound-endpoint port="587"` | `Mail.outboundAdapter(host)` (`MailSendingMessageHandler`) with `MailHeaders.FROM/TO/SUBJECT` | STARTTLS on by default (`smtp.starttls`), auth when `smtp.user` is set; body = payload string, text/plain |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `smtp.host` / `smtp.user` / `smtp.password` | same keys | smtp.gmail.com / blank / blank |
| endpoint `port="587"` | `smtp.port` | 587 |
| Gmail connector (STARTTLS) | `smtp.starttls` | true |
| `mail.from` / `mail.to` / `mail.subject` | same keys | blank |
| endpoint `path` / default polling | `file.input-dir` / `file.polling-frequency` | src/main/resources/input / 1000 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-sample-orders-emailed-as-list` | Mule input.csv → email whose body equals `src/main/resources/email.txt` (the IT's assertion) plus subject/from/to; file deleted | munit |
| `02-decimal-totals-and-single-email-per-file` | Number rendering for decimal and integral totals, one email per file | documented |

## Behavioural deltas

None.
