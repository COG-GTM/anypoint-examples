# Upload to FTP after converting JSON to XML (Spring Boot 3)

Spring Boot 3 / Java 17 migration of the Mule 3.8 example [`upload-to-ftp-after-converting-json-to-xml`](../../upload-to-ftp-after-converting-json-to-xml).

`POST /` with the employees JSON. The body is converted to XML (port of the DataWeave script), uploaded to the FTP server as `/Upload/muleExample.xml` through a Spring Integration `RemoteFileTemplate`, and returned in the HTTP response.

## Run

```bash
cd spring-boot-migration/upload-to-ftp-after-converting-json-to-xml
mvn spring-boot:run   # then: curl -X POST -H 'Content-Type: application/json' -d @../../upload-to-ftp-after-converting-json-to-xml/src/test/resources/message.json http://localhost:8081/
```

## Tests

```bash
cd spring-boot-migration
mvn -pl upload-to-ftp-after-converting-json-to-xml -am verify     # needs Docker for Testcontainers-based modules
```

`FtpUploadParityTest` runs every fixture over **FTP** against an embedded Apache FtpServer with user `demo`/`demo` and an `/Upload/` directory, then reads the uploaded file from the server's home directory. `SftpUploadParityTest` runs the same fixtures with `ftp.protocol=sftp` against the `atmoz/sftp` Testcontainer (image from `ParityContainers`, configured with `demo:demo:::Upload`) and reads the file back over an independent SFTP session. `EmployeesJsonToXmlTest` unit-tests escaping and the XML declaration.

Fixtures live in `src/test/resources/parity/upload-to-ftp-after-converting-json-to-xml/`. Each fixture's `expected` (and any side-effect block) holds the **Mule** app's behaviour, and `source.reference` cites where it comes from. See [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) for the construct mapping.

## Behavioural deltas

- The FTP endpoint values that Mule hard-coded in the flow XML are now properties with the same defaults. `ftp.protocol=sftp` is an opt-in addition; the default stays plain FTP, as in Mule.
- Error responses (a non-JSON body, an FTP failure) are Spring Boot's default 500 error body, not Mule's exception text. No fixture covers them.
