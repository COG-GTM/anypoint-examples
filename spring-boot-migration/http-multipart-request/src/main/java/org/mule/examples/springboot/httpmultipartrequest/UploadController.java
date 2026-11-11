package org.mule.examples.springboot.httpmultipartrequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final Path uploadDirectory;

    public UploadController(@Value("${upload.directory:/tmp}") Path uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    /** Mule flow {@code uploadFileFlow}: parse-template location="uploadFile.html". */
    @GetMapping(path = "/uploadFile", produces = MediaType.TEXT_HTML_VALUE)
    public String page() throws IOException {
        return StreamUtils.copyToString(new ClassPathResource("uploadFile.html").getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Mule flow {@code processFileFlow}: take attachment {@code file}, log its Content-Type at ERROR, write it to
     * {@code /tmp/<filename>} with the file outbound endpoint and echo its bytes back with status 200.
     */
    @PostMapping(path = "/uploadFile")
    public ResponseEntity<byte[]> upload(MultipartHttpServletRequest request) throws IOException {
        MultipartFile file = request.getFile("file");
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        log.error("{}", file.getContentType());
        byte[] content = file.getBytes();
        String fileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
        Files.createDirectories(uploadDirectory);
        Files.write(uploadDirectory.resolve(fileName), content);
        return ResponseEntity.ok().body(content);
    }
}
