# Importing a CSV file into Microsoft SharePoint (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`importing-a-csv-file-into-ms-sharepoint`](../../importing-a-csv-file-into-ms-sharepoint).

Polls `src/main/resources/input` every 10 s and moves each file to `src/main/resources/output`. It creates `/Shared Documents/${csvImportFolder}` in SharePoint Online and uploads the content as `/Shared Documents/${csvImportFolder}/contacts.csv` (overwrite), through a typed SharePoint REST client.

## Run

```bash
cd spring-boot-migration/importing-a-csv-file-into-ms-sharepoint
mvn spring-boot:run -DSharepoint.SiteUrl=https://<company>.sharepoint.com -Dsharepoint.access-token=<token> -DcsvImportFolder=my_folder
```

## Tests

```bash
cd spring-boot-migration
mvn -pl importing-a-csv-file-into-ms-sharepoint -am verify     # needs Docker for Testcontainers-based modules
```

`CsvToSharePointParityTest` points `Sharepoint.SiteUrl` at WireMock (`WireMockParitySupport`), registers each fixture's `downstream` stubs, and drops the input file. It then verifies `expectedOutbound`: the folder-create JSON body, the file-add query aliases and raw CSV body, the bearer token, and that no unstubbed SharePoint call was made. `expected` describes the file left in the move-to directory.

Fixtures live in `src/test/resources/parity/importing-a-csv-file-into-ms-sharepoint/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- **Authentication**: the Mule connector logged in to SharePoint Online with `Sharepoint.Username`/`Sharepoint.Password` (legacy claims-based login). The REST client sends an OAuth bearer token from `sharepoint.access-token` instead, e.g. from an Entra ID app registration. The calls and their order are the same. The username and password properties are kept for reference and are not used.
- The SharePoint connector itself is proprietary, so it is replaced by the `SharePointClient` interface plus a REST implementation. Parity is proven against a WireMock SharePoint, not a live tenant.
