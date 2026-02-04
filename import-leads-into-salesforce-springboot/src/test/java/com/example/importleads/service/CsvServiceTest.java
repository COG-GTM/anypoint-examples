package com.example.importleads.service;

import com.example.importleads.exception.CsvProcessingException;
import com.example.importleads.model.Lead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvServiceTest {

    private CsvService csvService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        csvService = new CsvService();
    }

    @Test
    void parseLeadsFromCsv_validFile_returnsLeads() throws IOException {
        String csvContent = """
                Company,FirstName,LastName,Birthday,Email
                Acme Corp,John,Doe,01/01/1990,john.doe@acme.com
                Tech Inc,Jane,Smith,02/15/1985,jane.smith@tech.com
                """;
        
        Path csvFile = tempDir.resolve("test-leads.csv");
        Files.writeString(csvFile, csvContent);

        List<Lead> leads = csvService.parseLeadsFromCsv(csvFile);

        assertEquals(2, leads.size());
        
        Lead firstLead = leads.get(0);
        assertEquals("Acme Corp", firstLead.getCompany());
        assertEquals("John", firstLead.getFirstName());
        assertEquals("Doe", firstLead.getLastName());
        assertEquals("john.doe@acme.com", firstLead.getEmail());
        
        Lead secondLead = leads.get(1);
        assertEquals("Tech Inc", secondLead.getCompany());
        assertEquals("Jane", secondLead.getFirstName());
        assertEquals("Smith", secondLead.getLastName());
        assertEquals("jane.smith@tech.com", secondLead.getEmail());
    }

    @Test
    void parseLeadsFromCsv_emptyFile_returnsEmptyList() throws IOException {
        String csvContent = "Company,FirstName,LastName,Birthday,Email\n";
        
        Path csvFile = tempDir.resolve("empty-leads.csv");
        Files.writeString(csvFile, csvContent);

        List<Lead> leads = csvService.parseLeadsFromCsv(csvFile);

        assertTrue(leads.isEmpty());
    }

    @Test
    void parseLeadsFromCsv_nonExistentFile_throwsException() {
        Path nonExistentFile = tempDir.resolve("non-existent.csv");

        assertThrows(CsvProcessingException.class, () -> {
            csvService.parseLeadsFromCsv(nonExistentFile);
        });
    }

    @Test
    void parseLeadsFromCsv_fileWithMissingFields_handlesGracefully() throws IOException {
        String csvContent = """
                Company,FirstName,LastName,Birthday,Email
                Acme Corp,John,Doe,,john.doe@acme.com
                """;
        
        Path csvFile = tempDir.resolve("partial-leads.csv");
        Files.writeString(csvFile, csvContent);

        List<Lead> leads = csvService.parseLeadsFromCsv(csvFile);

        assertEquals(1, leads.size());
        Lead lead = leads.get(0);
        assertEquals("Acme Corp", lead.getCompany());
        assertEquals("john.doe@acme.com", lead.getEmail());
    }
}
