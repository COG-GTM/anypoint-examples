package com.example.importleads.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingResultTest {

    @Test
    void processingResult_constructor_setsAllFields() {
        ProcessingResult result = new ProcessingResult("test.csv", 100, 80, 10, 10);
        
        assertEquals("test.csv", result.getFileName());
        assertEquals(100, result.getTotalRecords());
        assertEquals(80, result.getLoadedRecords());
        assertEquals(10, result.getFailedRecords());
        assertEquals(10, result.getSkippedRecords());
    }

    @Test
    void processingResult_toString_containsAllInfo() {
        ProcessingResult result = new ProcessingResult("leads.csv", 50, 45, 3, 2);
        
        String toString = result.toString();
        
        assertTrue(toString.contains("leads.csv"));
        assertTrue(toString.contains("50"));
        assertTrue(toString.contains("45"));
        assertTrue(toString.contains("3"));
        assertTrue(toString.contains("2"));
    }

    @Test
    void processingResult_zeroRecords_handledCorrectly() {
        ProcessingResult result = new ProcessingResult("empty.csv", 0, 0, 0, 0);
        
        assertEquals(0, result.getTotalRecords());
        assertEquals(0, result.getLoadedRecords());
        assertEquals(0, result.getFailedRecords());
        assertEquals(0, result.getSkippedRecords());
    }
}
