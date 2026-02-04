package com.example.multipart.controller;

import com.example.multipart.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    void getUploadForm_ShouldReturnHtmlForm() throws Exception {
        mockMvc.perform(get("/uploadFile"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<form")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("multipart/form-data")));
    }

    @Test
    void uploadFile_ShouldReturnFileContent() throws Exception {
        byte[] fileContent = "Hello, World!".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                fileContent
        );

        when(fileStorageService.storeFile(any())).thenReturn(fileContent);

        mockMvc.perform(multipart("/uploadFile").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes(fileContent));
    }

    @Test
    void uploadFile_WithImageFile_ShouldReturnCorrectContentType() throws Exception {
        byte[] imageContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                imageContent
        );

        when(fileStorageService.storeFile(any())).thenReturn(imageContent);

        mockMvc.perform(multipart("/uploadFile").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(imageContent));
    }
}
