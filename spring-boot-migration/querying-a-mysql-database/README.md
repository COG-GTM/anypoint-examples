# Querying a MySQL database: Spring Boot migration

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`querying-a-mysql-database`](../../querying-a-mysql-database).

`GET /?lastname=<name>` returns the `first_name` of each employee with that last name as a JSON array (`[{"first_name":"Chava"},…]`). Data access uses Spring Data JDBC.

See [MIGRATION-NOTES.md](MIGRATION-NOTES.md) for the Mule construct → Spring mapping.

## Run

```bash
cd spring-boot-migration/querying-a-mysql-database
mvn spring-boot:run
```

```bash
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:5.7   # then load ../../querying-a-mysql-database/src/main/resources/import.sql
mvn spring-boot:run
curl 'http://localhost:8081/?lastname=Puckett'
```

## Tests

```bash
cd spring-boot-migration
mvn -pl querying-a-mysql-database -am verify
```

The parity fixtures under `src/test/resources/parity/querying-a-mysql-database/` describe the Mule app's behaviour. Each one cites its source in `source.reference`. They run against the Spring Boot app on every build, and also against a running Mule 3.8 app when `-Dparity.mule.base-url=...` is set (see [`parity-test-support`](../parity-test-support/README.md)).

## Behavioural deltas

- The Mule dynamic query concatenated `lastname` into the SQL, so a value containing quotes changed the statement (SQL injection). Spring binds it as a parameter. For every plain last name the results are identical.
- A missing `lastname` is bound as the text `null`, which is what the Mule MEL template produced. Both return `[]`.
