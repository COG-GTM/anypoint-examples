# Migration notes: `querying-a-mysql-database`

- Original Mule app: [`../../querying-a-mysql-database`](../../querying-a-mysql-database)
- Mule flows: `database-to-json.xml` → `database-to-jsonFlow`
- Spring Boot module: `spring-boot-migration/querying-a-mysql-database`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/"` | `DatabaseToJsonController#query` (`@RequestMapping("/")`) |  |
| `db:generic-config url="${jdbc.url}"` (MySQL) | `spring.datasource.url: ${jdbc.url}` + `mysql-connector-j` |  |
| `db:select` / `db:dynamic-query` `select first_name from employees where last_name = "#[...lastname]"` | `EmployeeRepository#findFirstNamesByLastName` (Spring Data JDBC `@Query`, bound parameter), `Employee` mapped to `employees` | No `ORDER BY` in either; MySQL returns primary-key order |
| `json:object-to-json-transformer` | Jackson serialisation of `List<Map<String,Object>>` with key `first_name` |  |
| `QueryingMysqlDatabaseIT` + `MySQLDbCreator` | `QueryingAMysqlDatabaseParityTest` with a Testcontainers MySQL 5.7 (`ParityContainers.mysql()`, `@ServiceConnection`) seeded from `import.sql` |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port}`) | 8081 |
| `jdbc.url` | `spring.datasource.url` (`${jdbc.url}`) | `jdbc:mysql://localhost:3306/company?user=generatedata&password=generatedata` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-lastname-puckett.json` | lastname=Puckett returns the first names of both Puckett employees as a JSON array, in primary-key order. | `documented` |
| `02-lastname-sosa.json` | lastname=Sosa matches a single seeded employee. | `documented` |
| `03-lastname-no-match.json` | A last name with no rows yields an empty JSON array (object-to-json of an empty result list). | `documented` |
| `04-lastname-missing.json` | Without the lastname parameter the MEL template renders null, the query matches no employee and the response is an empty JSON array. | `documented` |

## Behavioural deltas

- The Mule dynamic query concatenated `lastname` into the SQL, so a value containing quotes changed the statement (SQL injection). Spring binds it as a parameter. For every plain last name the results are identical.
- A missing `lastname` is bound as the text `null`, which is what the Mule MEL template produced. Both return `[]`.
