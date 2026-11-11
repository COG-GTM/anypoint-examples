# Migration notes: `http-multipart-request`

- Original Mule app: [`../../http-multipart-request`](../../http-multipart-request)
- Mule flows: `http-multipart-request.xml` → `uploadFileFlow`, `processFileFlow`
- Spring Boot module: `spring-boot-migration/http-multipart-request`

## Mule constructs → Spring equivalents

| Mule construct (file:element / flow) | Spring Boot equivalent (class / config) | Notes |
|---|---|---|
| `http:listener allowedMethods="GET" path="/uploadFile"` + `parse-template location="uploadFile.html"` | `UploadController#page` (`@GetMapping`, classpath `uploadFile.html`) | Template copied verbatim (it has no MEL) |
| `http:listener allowedMethods="POST" path="/uploadFile"` + `http:response-builder statusCode=200 reasonPhrase=...` | `UploadController#upload` (`@PostMapping`, `MultipartHttpServletRequest`) | No reason phrase (see deltas) |
| `expression-component` that parses the filename from `Content-Disposition` | `MultipartFile#getOriginalFilename()`, reduced to its base name |  |
| `logger level=ERROR` with the part's `Content-Type` | `log.error("{}", file.getContentType())` |  |
| `set-payload #[...dataSource.content]` | `ResponseEntity.ok().body(file.getBytes())` |  |
| `file:outbound-endpoint path="/tmp" outputPattern="#[attachmentFileName]"` | `Files.write(upload.directory/<name>)` |  |

## Configuration

| Mule property | Spring property | Default |
|---|---|---|
| `http.port` | `server.port` (`${http.port:8081}`) | 8081 |
| (hard-coded `/tmp`) | `upload.directory` | `/tmp` |
| (no limit) | `upload.max-file-size` / `upload.max-request-size` | `100MB` |

## Parity coverage

| Fixture | What it proves | Evidence (`documented` / `recorded` / `munit`) |
|---|---|---|
| 01-get-upload-page | Form HTML verbatim | munit |
| 02-upload-text-file | 200, echoed content, `test.txt` written | munit |
| 03-upload-file-named-file | MUnit's `/tmp/file` naming | munit |
| 04-upload-without-file-part | 500, nothing written | documented |
| 05-put-not-allowed | PUT → 405 | documented |

## Behavioural deltas

- **HTTP reason phrase.** Mule sets a custom reason phrase (`File was uploaded.` on the 200). Tomcat (Spring Boot's embedded server) does not send reason phrases, so the status line is just `200`. Status codes are identical; clients that read the reason phrase see a difference.
- **Stored file name is reduced to its base name.** Mule writes `/tmp/<filename>` using the raw `Content-Disposition` filename, so a name like `../../etc/x` would escape `/tmp`. Spring keeps only the last path segment (`Paths.get(name).getFileName()`). Ordinary file names are stored the same way.
- **Upload directory is configurable.** It is `upload.directory` and defaults to `/tmp`, as in Mule. The parity test points it at a temp directory.
- **Upload size limit.** Mule sets no multipart limit. Spring needs one, so it defaults to 100MB per file and per request (`upload.max-file-size`, `upload.max-request-size`). Larger uploads are rejected.
- **Upload never follows symlinks.** Mule's file endpoint follows a symlink at `/tmp/<filename>`. Spring opens the target with `NOFOLLOW_LINKS`, so the upload fails instead of overwriting the link target.
- **Response `Content-Type` isn't asserted.** Mule doesn't document what it sends for the attachment content. Spring sends `application/octet-stream`.
- **Body of the 500 when the `file` part is missing.** Mule writes its exception message. Spring sends an empty body with the same status, 500.
- **Added springdoc endpoints.** `/v3/api-docs` and `/swagger-ui.html` are new paths.
