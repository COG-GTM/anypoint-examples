# Importing a CSV file into MongoDB (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`importing-a-CSV-file-into-Mongo-DB`](../../importing-a-CSV-file-into-Mongo-DB).

Polls `src/main/resources/input` for CSV files. Each row becomes a `{firstname, surname, phone, email}` document in the `customers` collection, which is created first if it does not exist. The file is deleted after it is read.

## Run

```bash
cd spring-boot-migration/importing-a-CSV-file-into-Mongo-DB
mvn spring-boot:run   # then copy ../../importing-a-CSV-file-into-Mongo-DB/src/main/resources/input.csv into src/main/resources/input/
```

## Tests

```bash
cd spring-boot-migration
mvn -pl importing-a-CSV-file-into-Mongo-DB -am verify     # needs Docker for Testcontainers-based modules
```

`CsvToMongoParityTest` uses the MongoDB Testcontainer (`ParityContainers.mongo()`). For each fixture it drops the database, optionally pre-creates `customers` (`given.existingCollection`), drops `request.body` into a temporary input directory as `request.headers.File-Name`, waits for the file to be deleted, and compares the collection contents (without the generated `_id`, which is checked to be an `ObjectId`) with `expected.body` (STRICT JSON). It also checks the `Collection` and `Input-File-Deleted` facts in `expected.headers`.

Fixtures live in `src/test/resources/parity/importing-a-CSV-file-into-Mongo-DB/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

None.
