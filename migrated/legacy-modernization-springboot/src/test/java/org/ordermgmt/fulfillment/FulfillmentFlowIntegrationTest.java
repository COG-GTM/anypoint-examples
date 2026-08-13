package org.ordermgmt.fulfillment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * End-to-end equivalent of {@code LegacyModernizationIT}: post the sample envelope and assert that
 * the legacy drop folder receives the CSV file produced by the asynchronous branch.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FulfillmentFlowIntegrationTest {

    private static Path outputDirectory;

    @DynamicPropertySource
    static void legacyOutputDirectory(DynamicPropertyRegistry registry) throws Exception {
        outputDirectory = Files.createTempDirectory("legacy-fulfillment-output");
        registry.add("legacy.fulfillment.output-directory", () -> outputDirectory.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void writesTheOrderToTheLegacyDropFolder() throws Exception {
        mockMvc.perform(post("/OrderFulfillment").contentType(MediaType.TEXT_XML).content(SoapFixtures.REQUEST))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            try (var files = Files.list(outputDirectory)) {
                List<Path> written = files.toList();
                assertThat(written).hasSize(1);
                assertThat(Files.readString(written.get(0)))
                        .startsWith("MSKU,QTY,")
                        .contains("1234,500,Mulesoft,77 Geary St Level 4");
            }
        });
    }
}
