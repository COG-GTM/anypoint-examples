package org.mule.examples.springboot.httpmultipartrequest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultipartSizeAndSymlinkTest {

    static final Path UPLOAD_DIR = createTempDir();

    @DynamicPropertySource
    static void uploadDir(DynamicPropertyRegistry registry) {
        registry.add("upload.directory", UPLOAD_DIR::toString);
    }

    @Autowired
    TestRestTemplate rest;

    @Test
    void uploadsLargerThanSpringsDefaultOneMegabyteAreStoredAndEchoed() throws IOException {
        byte[] content = new byte[2 * 1024 * 1024];
        Arrays.fill(content, (byte) 'x');
        ResponseEntity<byte[]> response = upload("big.bin", content);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(content.length);
        assertThat(Files.size(UPLOAD_DIR.resolve("big.bin"))).isEqualTo(content.length);
    }

    @Test
    void uploadDoesNotFollowASymlinkPlantedInTheUploadDirectory() throws IOException {
        Path outside = Files.createTempFile("outside-", ".txt");
        Files.writeString(outside, "original");
        Files.createSymbolicLink(UPLOAD_DIR.resolve("link.txt"), outside);
        ResponseEntity<byte[]> response = upload("link.txt", "overwritten".getBytes());
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(outside).hasContent("original");
    }

    private ResponseEntity<byte[]> upload(String fileName, byte[] content) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.postForEntity("/uploadFile", new HttpEntity<>(body, headers), byte[].class);
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("multipart-limits-");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
