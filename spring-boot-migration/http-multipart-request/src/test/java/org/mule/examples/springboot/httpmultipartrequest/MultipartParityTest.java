package org.mule.examples.springboot.httpmultipartrequest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Besides the HTTP contract, checks the file-outbound side effect: the uploaded part lands in the upload directory
 * (Mule: {@code file:outbound-endpoint path="/tmp" outputPattern="#[attachmentFileName]"}).
 */
class MultipartParityTest extends AbstractParityTest {

    static final Path UPLOAD_DIR = createTempDir();

    @DynamicPropertySource
    static void uploadDir(DynamicPropertyRegistry registry) {
        registry.add("upload.directory", UPLOAD_DIR::toString);
    }

    @Override
    protected String fixtureDirectory() {
        return "parity/http-multipart-request";
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        try (var files = Files.list(UPLOAD_DIR)) {
            for (Path p : files.toList()) {
                Files.delete(p);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        try {
            switch (fixture.name()) {
                case "02-upload-text-file" -> assertThat(UPLOAD_DIR.resolve("test.txt"))
                        .hasContent("file uploaded");
                case "03-upload-file-named-file" -> assertThat(Files.readString(UPLOAD_DIR.resolve("file"),
                        StandardCharsets.UTF_8)).isEqualTo("file uploaded");
                default -> {
                    try (var files = Files.list(UPLOAD_DIR)) {
                        assertThat(files).as("no file written for %s", fixture.name()).isEmpty();
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("http-multipart-request-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
