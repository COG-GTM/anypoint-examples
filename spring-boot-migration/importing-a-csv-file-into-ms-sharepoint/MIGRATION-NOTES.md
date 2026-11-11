# Migration notes: `importing-a-csv-file-into-ms-sharepoint`

- Original Mule app: [`../../importing-a-csv-file-into-ms-sharepoint`](../../importing-a-csv-file-into-ms-sharepoint)
- Mule flows: `importing-a-csv-file-into-ms-sharepoint.xml` → `importing-a-csv-file-into-ms-sharepointFlow`
- Spring Boot module: `spring-boot-migration/importing-a-csv-file-into-ms-sharepoint`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `file:inbound-endpoint` (`pollingFrequency=10000`, `moveToDirectory`) | `CsvToSharePointFlowConfiguration`: `Files.inboundAdapter` + poller; the file is read, then moved to `file.move-to-dir` | Mule moves the file as it reads it, before the rest of the flow runs |
| `set-variable fileContent` | local `byte[] fileContent` |  |
| `sharepoint:online-connection-config` | `SharePointRestClient` (`RestClient`, `Accept: application/json;odata=verbose`, bearer token) | Typed client behind the `SharePointClient` interface; the proprietary connector is not available for Spring |
| `sharepoint:folder-create url="/Shared Documents/${csvImportFolder}"` | `SharePointClient#folderCreate` → `POST /_api/web/folders` (`SP.Folder` body) |  |
| `sharepoint:file-add fileServerRelativeUrl=".../contacts.csv" overwrite="true"` | `SharePointClient#fileAdd` → `POST /_api/web/GetFolderByServerRelativeUrl(@folder)/Files/add(url=@file,overwrite=true)` | Parameter aliases (`@folder`, `@file`) keep spaces and slashes out of the path |
| `logger` | SLF4J, same message |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `Sharepoint.SiteUrl` | `Sharepoint.SiteUrl` | blank |
| `Sharepoint.Username` / `Sharepoint.Password` | kept, unused (see deltas) | blank |
| – (new) | `sharepoint.access-token` | blank |
| `csvImportFolder` | `csvImportFolder` | my_folder |
| endpoint `path` / `moveToDirectory` / `pollingFrequency` | `file.input-dir` / `file.move-to-dir` / `file.polling-frequency` | src/main/resources/input / src/main/resources/output / 10000 |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-contacts-csv-uploaded-to-shared-documents-folder` | Sample contacts.csv: folder-create, then file-add with identical content (the IT compares content read back from SharePoint), and the file is moved | munit |
| `02-any-input-file-name-is-stored-as-contacts-csv` | Fixed SharePoint file name; the local file keeps its name in the output folder | documented |

## Behavioural deltas

- **Authentication**: the Mule connector logged in to SharePoint Online with `Sharepoint.Username`/`Sharepoint.Password` (legacy claims-based login). The REST client sends an OAuth bearer token from `sharepoint.access-token` instead, e.g. from an Entra ID app registration. The calls and their order are the same. The username and password properties are kept for reference and are not used.
- The SharePoint connector itself is proprietary, so it is replaced by the `SharePointClient` interface plus a REST implementation. Parity is proven against a WireMock SharePoint, not a live tenant.
