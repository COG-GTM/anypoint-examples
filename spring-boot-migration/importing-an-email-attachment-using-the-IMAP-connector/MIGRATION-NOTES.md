# Migration notes: `importing-an-email-attachment-using-the-IMAP-connector`

- Original Mule app: [`../../importing-an-email-attachment-using-the-IMAP-connector`](../../importing-an-email-attachment-using-the-IMAP-connector)
- Mule flows: `imap-to-xml.xml` → `imap-to-csvFlow1`
- Spring Boot module: `spring-boot-migration/importing-an-email-attachment-using-the-IMAP-connector`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `imaps:connector checkFrequency="100"` + `imaps:inbound-endpoint port="993"` | `ImapToXmlFlowConfiguration`: `Mail.imapInboundAdapter("imaps://...")` + 100 ms poller, `shouldDeleteMessages(true)` | Mule mail connectors default to `deleteReadMessages="true"`. The folder is closed (expunged) once the attachment has been read |
| `expression-transformer` `attachments-list` + `set-payload #[payload[0].getContent()]` | `MailAttachments#first` | Same keys as Mule 3's `MailUtils.getAttachments` (description, else file name, else counter), in sorted order |
| `dw:transform-message` "Map CSV to XML" | `OrdersCsvToXml#transform` (Commons CSV + StAX) | Values stay text; missing column → empty element |
| `logger` "Received: ..." | SLF4J, same message |  |
| `file:outbound-endpoint path="src/test/resources/output" outputPattern="orders.xml"` | `Files.writeString` to `file.output-dir/file.output-pattern` | Overwrites, like Mule's default `outputAppend=false` |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `imap.host` / `imap.user` / `imap.password` | same keys | blank |
| endpoint `port="993"` (imaps) | `imap.port` / `imap.protocol` | 993 / imaps |
| connector `checkFrequency` | `imap.check-frequency` | 100 |
| – (folder) | `imap.folder` | INBOX |
| file endpoint `path` / `outputPattern` | `file.output-dir` / `file.output-pattern` | src/test/resources/output / orders.xml |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-csv-attachment-converted-to-orders-xml` | The IT's email (attachment `report`, application/octet-stream, input.csv) → orders.xml content; message removed from the mailbox | munit |
| `02-quoted-csv-and-xml-escaping` | Quoted CSV values, XML escaping, orders.xml overwritten | documented |

## Behavioural deltas

- The protocol and port are configurable (`imap.protocol`, `imap.port`). The defaults keep Mule's IMAPS on 993, and the tests use plain IMAP against GreenMail.
- Attachment selection for messages with several parts follows Mule 3's `MailUtils.getAttachments` keying as read from the Mule source. Only single-attachment emails (what the Mule IT sends) are covered by fixtures.
