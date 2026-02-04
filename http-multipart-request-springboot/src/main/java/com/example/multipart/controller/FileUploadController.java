package com.example.multipart.controller;

import com.example.multipart.service.FileStorageService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class FileUploadController {

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping(value = "/uploadFile", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getUploadForm() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/uploadFile.html");
        String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(html);
    }

    @PostMapping("/uploadFile")
    public ResponseEntity<byte[]> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] fileContent = fileStorageService.storeFile(file);
        
        String contentType = file.getContentType();
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(fileContent);
    }
}
