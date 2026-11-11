# Querying a DB and attaching results to an email (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`querying-a-db-and-attaching-results-to-an-email`](../../querying-a-db-and-attaching-results-to-an-email).

`POST /` with `<employee>First Last</employee>` elements. Each employee is looked up in the MySQL `employees` table, and the results are aggregated into a CSV. The CSV is sent as an email (text body + `employees.csv` attachment) and also returned in the HTTP response.

## Run

```bash
cd spring-boot-migration/querying-a-db-and-attaching-results-to-an-email
mvn spring-boot:run -Djdbc.url='jdbc:mysql://localhost:3306/company?user=...&password=...' -Dsmtp.user=... -Dsmtp.password=... -Dmail.from=... -Dmail.to=... -Dmail.subject=...
```

## Tests

```bash
cd spring-boot-migration
mvn -pl querying-a-db-and-attaching-results-to-an-email -am verify     # needs Docker for Testcontainers-based modules
```

`AttachmentsParityTest` (extends `AbstractParityTest`) runs MySQL 5.7 (`ParityContainers.mysql()`), seeded with `parity-db/employees.sql`: the schema from `import_orders.sql` plus the rows the Mule IT's `MySQLDbCreator` inserts. It also runs GreenMail. Each fixture's HTTP response is compared, then the delivered email is checked: `expectedEmail` (subject/from/to, text body) and `expectedAttachment` (`employees.csv`, text/plain, content). CSV line separators are matched as `\r?\n`, because only the line contents are documented on the Mule side.

Fixtures live in `src/test/resources/parity/querying-a-db-and-attaching-results-to-an-email/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- **SQL injection fix**: the Mule query concatenated `#[payload.getText().split(" ")[0]]` into the SQL string. The migration binds first and last name as JDBC parameters. Results are identical for ordinary names; names containing quotes no longer break or alter the query.
- An `<employee>` with no matching row produces a CSV line of empty values. This is not covered by a fixture, because the DataWeave output for a `null` `$[0]` is not documented.
