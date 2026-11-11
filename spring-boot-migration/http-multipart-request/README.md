# HTTP Multipart Request — Spring Boot

Spring Boot 3 migration of [`http-multipart-request`](../../http-multipart-request). `GET /uploadFile` serves an HTML upload form. `POST /uploadFile` (multipart, part `file`) stores the file in `/tmp/<filename>` and echoes its content with status 200.

## Run

```bash
cd spring-boot-migration
mvn -B -pl http-multipart-request -am verify          # build + parity tests
mvn -pl http-multipart-request spring-boot:run        # or: java -jar http-multipart-request/target/http-multipart-request-1.0.0-SNAPSHOT.jar
```

The port defaults to `8081` and honours the Mule property: `-Dhttp.port=9090`.

Open `http://localhost:8081/uploadFile` in a browser, or:

```bash
curl -F file=@test.txt http://localhost:8081/uploadFile
```

## Parity tests

`MultipartParityTest` replays the 5 fixtures in `src/test/resources/parity/http-multipart-request/` (form page, two uploads, missing part → 500, PUT → 405). It also checks the file-outbound side effect: after each upload it asserts the stored file and its content, and after the other cases it asserts that nothing was written.

Run against a live Mule runtime as well: `mvn -pl http-multipart-request -am verify -Dparity.mule.base-url=http://localhost:8081`.

## Behavioural deltas

- **HTTP reason phrase.** Mule sets a custom reason phrase (`File was uploaded.` on the 200). Tomcat (Spring Boot's embedded server) does not send reason phrases, so the status line is just `200`. Status codes are identical; clients that read the reason phrase see a difference.
- **Stored file name is reduced to its base name.** Mule writes `/tmp/<filename>` using the raw `Content-Disposition` filename, so a name like `../../etc/x` would escape `/tmp`. Spring keeps only the last path segment (`Paths.get(name).getFileName()`). Ordinary file names are stored the same way.
- **Upload directory is configurable.** It is `upload.directory` and defaults to `/tmp`, as in Mule. The parity test points it at a temp directory.
- **Upload size limit.** Mule sets no multipart limit. Spring needs one, so it defaults to 100MB per file and per request (`upload.max-file-size`, `upload.max-request-size`). Larger uploads are rejected.
- **Upload never follows symlinks.** Mule's file endpoint follows a symlink at `/tmp/<filename>`. Spring opens the target with `NOFOLLOW_LINKS`, so the upload fails instead of overwriting the link target.
- **Response `Content-Type` isn't asserted.** Mule doesn't document what it sends for the attachment content. Spring sends `application/octet-stream`.
- **Body of the 500 when the `file` part is missing.** Mule writes its exception message. Spring sends an empty body with the same status, 500.
- **Added springdoc endpoints.** `/v3/api-docs` and `/swagger-ui.html` are new paths.

Mule ↔ Spring mapping: [MIGRATION-NOTES.md](MIGRATION-NOTES.md).
