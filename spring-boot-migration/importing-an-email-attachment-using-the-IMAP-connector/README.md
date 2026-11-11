# Importing an email attachment using the IMAP connector (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`importing-an-email-attachment-using-the-IMAP-connector`](../../importing-an-email-attachment-using-the-IMAP-connector).

Polls a mailbox over IMAP (default IMAPS on port 993). The first attachment of each email is read as CSV, converted to an `<orders>` XML document and written to `src/test/resources/output/orders.xml`. Read messages are deleted from the server.

## Run

```bash
cd spring-boot-migration/importing-an-email-attachment-using-the-IMAP-connector
mvn spring-boot:run -Dimap.host=<host> -Dimap.user=<user> -Dimap.password=<password>
```

## Tests

```bash
cd spring-boot-migration
mvn -pl importing-an-email-attachment-using-the-IMAP-connector -am verify     # needs Docker for Testcontainers-based modules
```

`ImapToXmlParityTest` uses the GreenMail Testcontainer (`ParityContainers.greenMail()`) over plain IMAP (port 3143). Like the Mule IT, it sends `request.body` as the single attachment of an email (`Attachment-Name`, `Attachment-Content-Type`). It waits for `orders.xml`, compares it with `expected.body` (XML), and checks that no message is left in the mailbox (`Messages-Left-In-Inbox: 0`). `MailAttachmentsTest` covers the attachment keying and the no-attachment case.

Fixtures live in `src/test/resources/parity/importing-an-email-attachment-using-the-IMAP-connector/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The protocol and port are configurable (`imap.protocol`, `imap.port`). The defaults keep Mule's IMAPS on 993, and the tests use plain IMAP against GreenMail.
- Attachment selection for messages with several parts follows Mule 3's `MailUtils.getAttachments` keying as read from the Mule source. Only single-attachment emails (what the Mule IT sends) are covered by fixtures.
