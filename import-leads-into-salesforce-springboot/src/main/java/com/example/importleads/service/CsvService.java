package com.example.importleads.service;

import com.example.importleads.exception.CsvProcessingException;
import com.example.importleads.model.Lead;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;

@Service
public class CsvService {

    private static final Logger logger = LoggerFactory.getLogger(CsvService.class);

    public List<Lead> parseLeadsFromCsv(Path csvFile) {
        logger.info("Parsing CSV file: {}", csvFile);
        
        try (Reader reader = new FileReader(csvFile.toFile())) {
            List<Lead> leads = new CsvToBeanBuilder<Lead>(reader)
                    .withType(Lead.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
            
            logger.info("Successfully parsed {} leads from {}", leads.size(), csvFile.getFileName());
            return leads;
        } catch (IOException e) {
            throw new CsvProcessingException("Failed to read CSV file: " + csvFile, e);
        } catch (Exception e) {
            throw new CsvProcessingException("Failed to parse CSV file: " + csvFile, e);
        }
    }
}
