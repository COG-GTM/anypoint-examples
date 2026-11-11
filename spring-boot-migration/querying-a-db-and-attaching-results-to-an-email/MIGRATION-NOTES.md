# Migration notes: `querying-a-db-and-attaching-results-to-an-email`

- Original Mule app: [`../../querying-a-db-and-attaching-results-to-an-email`](../../querying-a-db-and-attaching-results-to-an-email)
- Mule flows: `attachments.xml` → `attachmentsFlow1`
- Spring Boot module: `spring-boot-migration/querying-a-db-and-attaching-results-to-an-email`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/"` | `AttachmentsController#queryAndEmail` (`@RequestMapping("/")`) |  |
| `splitter expression="#[xpath('//employee')]"` | DOM + XPath `//employee` (DOCTYPE disallowed) |  |
| `db:generic-config` + `db:select` dynamic query | `EmployeeRepository#findByName` (`JdbcTemplate`, bind parameters) | See deltas (SQL injection fix) |
| `collection-aggregator` | `List<List<Map<...>>>` in request order |  |
| `logger #[payload]` | SLF4J |  |
| `dw:transform-message` "Map Collection to CSV" (`header=true`, `$[0]`, `as :date`) | `EmployeesCsv#write` | `first_name,last_name,gender,dob,hire_date`, ISO dates |
| `object-to-string-transformer mimeType="text/csv"` | Response `Content-Type: text/csv;charset=UTF-8` |  |
| `set-attachment attachmentName="employees.csv" contentType="text/plain"` + `smtp:outbound-endpoint` | `EmployeeReportMailer` (`JavaMailSender`, flat `multipart/mixed`: text part + attachment) |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` | 8081 |
| `jdbc.url` | `jdbc.url` → `spring.datasource.url` | jdbc:mysql://localhost:3306/company |
| `smtp.host` / `smtp.port` / `smtp.user` / `smtp.password` | same keys → `spring.mail.*` | smtp.gmail.com / 587 / blank / blank |
| Gmail connector | `smtp.auth` / `smtp.starttls` | true / true |
| `mail.from` / `mail.to` / `mail.subject` | same keys | blank |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-three-employees-queried-and-emailed-as-csv` | IT request + IT DB rows → CSV in the response, email body and `employees.csv` attachment | munit |
| `02-single-employee-in-request-order` | Single employee, `//employee` at any depth | documented |

## Behavioural deltas

- **SQL injection fix**: the Mule query concatenated `#[payload.getText().split(" ")[0]]` into the SQL string. The migration binds first and last name as JDBC parameters. Results are identical for ordinary names; names containing quotes no longer break or alter the query.
- An `<employee>` with no matching row produces a CSV line of empty values. This is not covered by a fixture, because the DataWeave output for a `null` `$[0]` is not documented.
