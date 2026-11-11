# Migration notes: `document-integration-using-the-cmis-connector`

- Original Mule app: [`../../document-integration-using-the-cmis-connector`](../../document-integration-using-the-cmis-connector)
- Mule flows: `cmis-document-integration.xml` → `CMIS`
- Spring Boot module: `spring-boot-migration/document-integration-using-the-cmis-connector`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="cmis"` | `CmisController#createDocument` (`@RequestMapping("/cmis")`) | Any method, like the Mule listener |
| `scripting:component` Groovy "Load file" (`new FileInputStream("./src/main/resources/mulesoft-logo.jpg")`) | `cmis.document` resource (`classpath:mulesoft-logo.jpg`) |  |
| `cmis:config baseUrl repositoryId username password` | `OpenCmisClient` (Apache Chemistry OpenCMIS, AtomPub binding, lazy session) | Typed client behind the `CmisClient` interface |
| `cmis:create-document-by-path-from-content` | `CmisClient#createDocumentByPathFromContent` → `Session#getObjectByPath` + `Session#createDocument` |  |
| `json:object-to-json-transformer` (on the returned `ObjectId`) | `Map.of("id", id)` serialised by Jackson (`application/json`) |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` | 8081 |
| `cmis:config baseUrl` | `cmis.base-url` | http://demo.openkm.com/OpenKM/cmis/atom |
| `cmis:config repositoryId` | `cmis.repository-id` | default |
| `cmis:config username` / `password` | `cmis.username` / `cmis.password` | blank (see deltas) |
| operation `folderPath` | `cmis.folder-path` | /okm:root/ |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-post-uploads-logo-and-returns-object-id` | IT's POST: one create-document call with the documented attributes and the logo bytes (SHA-256); response `{"id":"/okm:root/pic<millis>.jpg"}` | munit |
| `02-browser-get-also-uploads` | README's browser GET runs the same upload | documented |

## Behavioural deltas

- **Stubbed repository in tests**: the OpenCMIS AtomPub client is not exercised against a live CMIS server in CI; the parity tests use the typed-client stub described above.
- The Mule config hard-coded the public OpenKM demo account (`user3`/`pass3`). The credentials are now properties and blank by default; set `cmis.username`/`cmis.password`.
- OpenCMIS' Apache CXF dependencies (only needed for its Web Services binding) are excluded.
