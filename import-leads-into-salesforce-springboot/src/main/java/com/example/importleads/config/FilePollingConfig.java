package com.example.importleads.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.polling")
public class FilePollingConfig {

    private String inputDirectory = "input";
    private String outputDirectory = "output";
    private long intervalMs = 10000;
    private String csvPattern = "*.csv";

    public String getInputDirectory() {
        return inputDirectory;
    }

    public void setInputDirectory(String inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public String getCsvPattern() {
        return csvPattern;
    }

    public void setCsvPattern(String csvPattern) {
        this.csvPattern = csvPattern;
    }
}
