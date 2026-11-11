# Migration notes: `importing-an-email-attachment-using-the-POP3-connector`

- Original Mule app: [`../../importing-an-email-attachment-using-the-POP3-connector`](../../importing-an-email-attachment-using-the-POP3-connector)
- Mule flows: `pop-to-xml.xml` → `pop-to-xmlFlow1`
- Spring Boot module: `spring-boot-migration/importing-an-email-attachment-using-the-POP3-connector`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `pop3s:connector checkFrequency="100"` + `pop3s:inbound-endpoint port="995"` | `PopToXmlFlowConfiguration`: `Mail.pop3InboundAdapter("pop3s://...")` + 100 ms poller, `shouldDeleteMessages(true)` | Mule mail connectors default to `deleteReadMessages="true"`. The folder is closed (expunged) once the attachment has been read |
| `expression-transformer` `attachments-list` + `set-payload #[payload[0].getContent()]` | `MailAttachments#first` | Same keys as Mule 3's `MailUtils.getAttachments` (description, else file name, else counter), in sorted order |
| `dw:transform-message` "Map CSV to XML" | `OrdersCsvToXml#transform` (Commons CSV + StAX) | Values stay text; missing column → empty element |
| `logger` "Received: ..." | SLF4J, same message |  |
| `file:outbound-endpoint path="src/test/resources/output" outputPattern="orders.xml"` | `Files.writeString` to `file.output-dir/file.output-pattern` | Overwrites, like Mule's default `outputAppend=false` |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `pop3.host` / `pop3.user` / `pop3.password` | same keys | blank |
| endpoint `port="995"` (pop3s) | `pop3.port` / `pop3.protocol` | 995 / pop3s |
| connector `checkFrequency` | `pop3.check-frequency` | 100 |
| – (folder) | `pop3.folder` | INBOX |
| file endpoint `path` / `outputPattern` | `file.output-dir` / `file.output-pattern` | src/test/resources/output / orders.xml |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-csv-attachment-converted-to-orders-xml` | The IT's email (attachment `report`, application/octet-stream, input.csv) → orders.xml content; message removed from the mailbox | munit |
| `02-quoted-csv-and-xml-escaping` | Quoted CSV values, XML escaping, orders.xml overwritten | documented |

## Behavioural deltas

- The protocol and port are configurable (`pop3.protocol`, `pop3.port`). The defaults keep Mule's POP3S on 995, and the tests use plain POP3 against GreenMail.
- Attachment selection for messages with several parts follows Mule 3's `MailUtils.getAttachments` keying as read from the Mule source. Only single-attachment emails (what the Mule IT sends) are covered by fixtures.
