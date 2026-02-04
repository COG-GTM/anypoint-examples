package com.example.importleads.controller;

import com.example.importleads.model.ProcessingResult;
import com.example.importleads.service.FilePollingService;
import com.example.importleads.service.SalesforceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadImportController {

    private static final Logger logger = LoggerFactory.getLogger(LeadImportController.class);

    private final FilePollingService filePollingService;
    private final SalesforceService salesforceService;

    public LeadImportController(FilePollingService filePollingService, SalesforceService salesforceService) {
        this.filePollingService = filePollingService;
        this.salesforceService = salesforceService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("salesforceConnected", salesforceService.isConnected());
        status.put("inputDirectory", filePollingService.getInputPath().toString());
        status.put("outputDirectory", filePollingService.getOutputPath().toString());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/upload")
    public ResponseEntity<ProcessingResult> uploadCsvFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path inputPath = filePollingService.getInputPath();
            Path targetFile = inputPath.resolve(originalFilename);
            Files.copy(file.getInputStream(), targetFile);
            logger.info("Uploaded file: {}", targetFile);

            ProcessingResult result = filePollingService.processFile(targetFile);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            logger.error("Failed to upload file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerPolling() {
        logger.info("Manually triggering file polling");
        filePollingService.pollForCsvFiles();
        return ResponseEntity.ok("Polling triggered successfully");
    }
}
