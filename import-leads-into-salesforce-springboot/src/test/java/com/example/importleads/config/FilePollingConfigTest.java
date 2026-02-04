package com.example.importleads.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilePollingConfigTest {

    @Test
    void defaultValues_areSetCorrectly() {
        FilePollingConfig config = new FilePollingConfig();
        
        assertEquals("input", config.getInputDirectory());
        assertEquals("output", config.getOutputDirectory());
        assertEquals(10000, config.getIntervalMs());
        assertEquals("*.csv", config.getCsvPattern());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        FilePollingConfig config = new FilePollingConfig();
        
        config.setInputDirectory("/custom/input");
        config.setOutputDirectory("/custom/output");
        config.setIntervalMs(5000);
        config.setCsvPattern("*.txt");
        
        assertEquals("/custom/input", config.getInputDirectory());
        assertEquals("/custom/output", config.getOutputDirectory());
        assertEquals(5000, config.getIntervalMs());
        assertEquals("*.txt", config.getCsvPattern());
    }
}
