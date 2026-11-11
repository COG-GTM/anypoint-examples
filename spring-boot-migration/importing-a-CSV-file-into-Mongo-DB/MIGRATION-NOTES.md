# Migration notes: `importing-a-CSV-file-into-Mongo-DB`

- Original Mule app: [`../../importing-a-CSV-file-into-Mongo-DB`](../../importing-a-CSV-file-into-Mongo-DB)
- Mule flows: `csv-to-mongodb.xml` → `csv-to-mongodbFlow1`
- Spring Boot module: `spring-boot-migration/importing-a-CSV-file-into-Mongo-DB`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `file:inbound-endpoint` "Poll files" (`path=src/main/resources/input`) | `CsvToMongoFlowConfiguration#csvToMongodbFlow`: `Files.inboundAdapter` + fixed-delay poller | Mule default `pollingFrequency` 1000 ms and `autoDelete=true` (file deleted after reading; the same name is read again later) |
| `dw:transform-message` "CSV to Map" | `CustomerCsv#parse` (Commons CSV) | Header row, values stay strings, a missing column becomes `null`, extra columns are dropped |
| `logger` "saving objects: #[payload]" | SLF4J `log.info("saving objects: {}", ...)` |  |
| `enricher` + `mongo:exists-collection`, `choice` → `mongo:create-collection` / logger | `CustomerImporter#importCsv`: `MongoTemplate#collectionExists` / `createCollection` | Logs "Customer Collection already exists." in the otherwise branch |
| `foreach` + `mongo:save-object-from-map` | `MongoTemplate#insert(new Document(map), "customers")` | One document per row, new `ObjectId` |
| `mongo:config` `database/username/password` | `spring.data.mongodb.*` + `MongoClientSettingsBuilderCustomizer` | Credentials applied only when `database.user` is set |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `database.name` | `database.name` → `spring.data.mongodb.database` | customers |
| `database.user` / `database.password` | same keys | blank (no auth) |
| connector default host/port | `database.host` / `database.port` | localhost / 27017 |
| endpoint `path` | `file.input-dir` | src/main/resources/input |
| connector default `pollingFrequency` | `file.polling-frequency` | 1000 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-sample-csv-creates-collection-and-saves-customer` | Mule sample input.csv → collection created, exactly one document (IT asserts count 1), file deleted | munit |
| `02-existing-collection-saves-every-row-in-order` | Otherwise branch, one document per row in order, extra column dropped | documented |
| `03-quoted-values-and-missing-column` | Quoted values, missing column stored as `null` | documented |

## Behavioural deltas

None.
