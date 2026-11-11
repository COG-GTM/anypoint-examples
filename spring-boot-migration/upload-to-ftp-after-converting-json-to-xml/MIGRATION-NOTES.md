# Migration notes: `upload-to-ftp-after-converting-json-to-xml`

- Original Mule app: [`../../upload-to-ftp-after-converting-json-to-xml`](../../upload-to-ftp-after-converting-json-to-xml)
- Mule flows: `upload-to-ftp.xml` → `main`
- Spring Boot module: `spring-boot-migration/upload-to-ftp-after-converting-json-to-xml`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener path="/"` (`main`) | `UploadToFtpController#jsonToXmlAndUpload` (`@RequestMapping("/")`) | Mule's listener accepts any method, and so does this mapping |
| `dw:transform-message` "JSON to XML" | `EmployeesJsonToXml#transform` (StAX) | Same element structure. Output starts with `<?xml version='1.0' encoding='UTF-8'?>` like DataWeave; `map` over a single object or a missing value behaves as in DW |
| `ftp:connector` + `ftp:outbound-endpoint` "Upload to FTP" | `FtpUploadConfiguration#uploadTemplate`: `FtpRemoteFileTemplate` (`DefaultFtpSessionFactory`) or `SftpRemoteFileTemplate` | Passive mode, binary type, no temporary file name, `FileExistsMode.REPLACE` (fixed `outputPattern` overwrites) |
| HTTP response = final payload | `ResponseEntity` with `application/xml;charset=UTF-8` | DataWeave `%output application/xml encoding="UTF-8"` |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| endpoint `host` (IT: `ftp.host`) | `ftp.host` | ftp-server.demo.solarwinds.com |
| endpoint `port` (IT: `ftp.port`) | `ftp.port` | 21 |
| endpoint `user` / `password` | `ftp.user` / `ftp.password` | demo / demo |
| endpoint `path` | `ftp.path` | /Upload/ |
| endpoint `outputPattern` | `ftp.output-pattern` | muleExample.xml |
| – (new) | `ftp.protocol` (`ftp` or `sftp`), `ftp.passive`, `ftp.allow-unknown-keys` | ftp, true, false |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| `01-employees-json-converted-to-xml-and-uploaded` | IT request → XML response (Content-Type, body) plus identical file at `/Upload/muleExample.xml` | munit |
| `02-second-upload-overwrites-muleExample-xml` | Overwrite of the fixed file name, single-object `map`, every address field copied, XML escaping | documented |

## Behavioural deltas

- The FTP endpoint values that Mule hard-coded in the flow XML are now properties with the same defaults. `ftp.protocol=sftp` is an opt-in addition; the default stays plain FTP, as in Mule.
- Error responses (a non-JSON body, an FTP failure) are Spring Boot's default 500 error body, not Mule's exception text. No fixture covers them.
