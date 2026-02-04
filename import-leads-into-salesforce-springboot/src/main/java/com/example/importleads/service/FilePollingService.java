package com.example.importleads.service;

import com.example.importleads.config.FilePollingConfig;
import com.example.importleads.model.Lead;
import com.example.importleads.model.ProcessingResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FilePollingService {

    private static final Logger logger = LoggerFactory.getLogger(FilePollingService.class);

    private final FilePollingConfig filePollingConfig;
    private final CsvService csvService;
    private final SalesforceService salesforceService;

    private Path inputPath;
    private Path outputPath;

    public FilePollingService(FilePollingConfig filePollingConfig, CsvService csvService, SalesforceService salesforceService) {
        this.filePollingConfig = filePollingConfig;
        this.csvService = csvService;
        this.salesforceService = salesforceService;
    }

    @PostConstruct
    public void init() {
        inputPath = Paths.get(filePollingConfig.getInputDirectory()).toAbsolutePath();
        outputPath = Paths.get(filePollingConfig.getOutputDirectory()).toAbsolutePath();

        try {
            Files.createDirectories(inputPath);
            Files.createDirectories(outputPath);
            logger.info("Input directory: {}", inputPath);
            logger.info("Output directory: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to create directories: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize file polling directories", e);
        }
    }

    @Scheduled(fixedDelayString = "${file.polling.interval-ms:10000}")
    public void pollForCsvFiles() {
        logger.debug("Polling for CSV files in: {}", inputPath);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, filePollingConfig.getCsvPattern())) {
            for (Path csvFile : stream) {
                if (Files.isRegularFile(csvFile)) {
                    processFile(csvFile);
                }
            }
        } catch (IOException e) {
            logger.error("Error polling directory: {}", e.getMessage());
        }
    }

    public ProcessingResult processFile(Path csvFile) {
        logger.info("Processing file: {}", csvFile.getFileName());

        try {
            List<Lead> leads = csvService.parseLeadsFromCsv(csvFile);
            int totalRecords = leads.size();

            if (leads.isEmpty()) {
                logger.warn("No leads found in file: {}", csvFile.getFileName());
                moveToOutput(csvFile);
                return new ProcessingResult(csvFile.getFileName().toString(), 0, 0, 0, 0);
            }

            Set<String> existingEmails = salesforceService.findExistingLeadEmails(leads);

            List<Lead> newLeads = new ArrayList<>();
            int skippedCount = 0;
            for (Lead lead : leads) {
                if (lead.getEmail() != null && existingEmails.contains(lead.getEmail().toLowerCase())) {
                    logger.debug("Skipping existing lead: {}", lead.getEmail());
                    skippedCount++;
                } else {
                    newLeads.add(lead);
                }
            }

            logger.info("Found {} new leads to create, {} already exist", newLeads.size(), skippedCount);

            int loadedCount = 0;
            int failedCount = 0;
            if (!newLeads.isEmpty()) {
                loadedCount = salesforceService.createLeads(newLeads);
                failedCount = newLeads.size() - loadedCount;
            }

            moveToOutput(csvFile);

            ProcessingResult result = new ProcessingResult(
                    csvFile.getFileName().toString(),
                    totalRecords,
                    loadedCount,
                    failedCount,
                    skippedCount
            );

            logger.info("{} Loaded Records, {} Failed Records, {} Skipped (already exist)",
                    loadedCount, failedCount, skippedCount);

            return result;

        } catch (Exception e) {
            logger.error("Error processing file {}: {}", csvFile.getFileName(), e.getMessage(), e);
            return new ProcessingResult(csvFile.getFileName().toString(), 0, 0, 1, 0);
        }
    }

    private void moveToOutput(Path sourceFile) {
        try {
            Path targetFile = outputPath.resolve(sourceFile.getFileName());
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Moved processed file to: {}", targetFile);
        } catch (IOException e) {
            logger.error("Failed to move file {} to output directory: {}", sourceFile.getFileName(), e.getMessage());
        }
    }

    public Path getInputPath() {
        return inputPath;
    }

    public Path getOutputPath() {
        return outputPath;
    }
}
