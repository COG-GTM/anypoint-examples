# Sending a CSV file through email using SMTP (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`sending-a-csv-file-through-email-using-smtp`](../../sending-a-csv-file-through-email-using-smtp).

Polls `src/main/resources/input` for CSV order files. Each file becomes a list of orders with numeric `pricePerUnit`, `units` and `totalPrice`, sent as one plain-text email over SMTP (Gmail-style STARTTLS on 587). The file is deleted after it is read.

## Run

```bash
cd spring-boot-migration/sending-a-csv-file-through-email-using-smtp
mvn spring-boot:run -Dsmtp.user=<user> -Dsmtp.password=<password> -Dmail.from=<from> -Dmail.to=<to> -Dmail.subject='Mule flow completed!'
```

## Tests

```bash
cd spring-boot-migration
mvn -pl sending-a-csv-file-through-email-using-smtp -am verify     # needs Docker for Testcontainers-based modules
```

`CsvToSmtpParityTest` points SMTP at the GreenMail Testcontainer and drops the input file. It reads the delivered message over IMAP and compares Subject/From/To/Content-Type and `Input-File-Deleted` (headers) and the trimmed text body (the IT also trims) with `expected`. It also checks that exactly one email was sent. `OrdersCsvTest` covers number narrowing.

Fixtures live in `src/test/resources/parity/sending-a-csv-file-through-email-using-smtp/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

None.
