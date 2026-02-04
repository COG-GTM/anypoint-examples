package com.example.importleads.model;

public class ProcessingResult {

    private final String fileName;
    private final int totalRecords;
    private final int loadedRecords;
    private final int failedRecords;
    private final int skippedRecords;

    public ProcessingResult(String fileName, int totalRecords, int loadedRecords, int failedRecords, int skippedRecords) {
        this.fileName = fileName;
        this.totalRecords = totalRecords;
        this.loadedRecords = loadedRecords;
        this.failedRecords = failedRecords;
        this.skippedRecords = skippedRecords;
    }

    public String getFileName() {
        return fileName;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getLoadedRecords() {
        return loadedRecords;
    }

    public int getFailedRecords() {
        return failedRecords;
    }

    public int getSkippedRecords() {
        return skippedRecords;
    }

    @Override
    public String toString() {
        return "ProcessingResult{" +
                "fileName='" + fileName + '\'' +
                ", totalRecords=" + totalRecords +
                ", loadedRecords=" + loadedRecords +
                ", failedRecords=" + failedRecords +
                ", skippedRecords=" + skippedRecords +
                '}';
    }
}
