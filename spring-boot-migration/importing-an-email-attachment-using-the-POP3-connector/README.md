# Importing an email attachment using the POP3 connector (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`importing-an-email-attachment-using-the-POP3-connector`](../../importing-an-email-attachment-using-the-POP3-connector).

Polls a mailbox over POP3 (default POP3S on port 995). The first attachment of each email is read as CSV, converted to an `<orders>` XML document and written to `src/test/resources/output/orders.xml`. Read messages are deleted from the server.

## Run

```bash
cd spring-boot-migration/importing-an-email-attachment-using-the-POP3-connector
mvn spring-boot:run -Dpop3.host=<host> -Dpop3.user=<user> -Dpop3.password=<password>
```

## Tests

```bash
cd spring-boot-migration
mvn -pl importing-an-email-attachment-using-the-POP3-connector -am verify     # needs Docker for Testcontainers-based modules
```

`PopToXmlParityTest` uses the GreenMail Testcontainer (`ParityContainers.greenMail()`) over plain POP3 (port 3110). Like the Mule IT, it sends `request.body` as the single attachment of an email (`Attachment-Name`, `Attachment-Content-Type`). It waits for `orders.xml`, compares it with `expected.body` (XML), and checks that no message is left in the mailbox (`Messages-Left-In-Inbox: 0`). `MailAttachmentsTest` covers the attachment keying and the no-attachment case.

Fixtures live in `src/test/resources/parity/importing-an-email-attachment-using-the-POP3-connector/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The protocol and port are configurable (`pop3.protocol`, `pop3.port`). The defaults keep Mule's POP3S on 995, and the tests use plain POP3 against GreenMail.
- Attachment selection for messages with several parts follows Mule 3's `MailUtils.getAttachments` keying as read from the Mule source. Only single-attachment emails (what the Mule IT sends) are covered by fixtures.
