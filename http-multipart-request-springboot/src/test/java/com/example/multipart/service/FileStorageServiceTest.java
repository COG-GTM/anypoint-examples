package com.example.multipart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void storeFile_ShouldSaveFileAndReturnContent() throws IOException {
        byte[] content = "Test content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content
        );

        byte[] result = fileStorageService.storeFile(file);

        assertArrayEquals(content, result);
        assertTrue(Files.exists(tempDir.resolve("test.txt")));
        assertArrayEquals(content, Files.readAllBytes(tempDir.resolve("test.txt")));
    }

    @Test
    void storeFile_WithEmptyFilename_ShouldThrowException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "",
                "text/plain",
                "content".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeFile(file));
    }

    @Test
    void storeFile_WithNullFilename_ShouldThrowException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                null,
                "text/plain",
                "content".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> fileStorageService.storeFile(file));
    }

    @Test
    void getStorageLocation_ShouldReturnConfiguredPath() {
        assertEquals(tempDir.toAbsolutePath().normalize(), fileStorageService.getStorageLocation());
    }
}
