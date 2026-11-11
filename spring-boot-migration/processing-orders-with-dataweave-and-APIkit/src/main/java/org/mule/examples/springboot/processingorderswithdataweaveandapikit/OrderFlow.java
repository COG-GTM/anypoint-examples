package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.charset.StandardCharsets;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;

import org.springframework.web.client.RestClient;

/**
 * books.xml {@code OrderFlow}: file:inbound-endpoint → http:request GET /api/currencies (enricher target
 * flowVars.currencies) → DataWeave → file:outbound-endpoint orders.json and report.csv.
 */
@Configuration
public class OrderFlow {

    private static final Logger log = LoggerFactory.getLogger(OrderFlow.class);

    private final ObjectMapper mapper;
    private final RestClient restClient;
    private final Environment environment;
    private final Path outputDir;

    public OrderFlow(ObjectMapper mapper, RestClient.Builder restClientBuilder, Environment environment,
                     @Value("${orders.output-dir}") Path outputDir) {
        this.mapper = mapper;
        this.restClient = restClientBuilder.build();
        this.environment = environment;
        this.outputDir = outputDir;
    }

    @Bean
    IntegrationFlow orderIntegrationFlow(@Value("${orders.input-dir}") File inputDir,
                                         @Value("${orders.polling-frequency:1000}") long pollingFrequency) {
        return IntegrationFlow
                .from(org.springframework.integration.file.dsl.Files.inboundAdapter(inputDir).autoCreateDirectory(true).preventDuplicates(false)
                                .regexFilter("^[^.].*"),
                        e -> e.poller(Pollers.fixedDelay(Duration.ofMillis(pollingFrequency))))
                .handle(File.class, (file, headers) -> {
                    process(file);
                    return null;
                })
                .get();
    }

    void process(File input) {
        try {
            List<OrderMapper.Item> items = OrdersXmlReader.read(input);
            Map<String, List<OrderMapper.CurrencyRatio>> currencies = mapper.readValue(
                    restClient.get().uri(currencyBaseUrl() + "/api/currencies").retrieve().body(String.class),
                    new TypeReference<>() { });
            java.nio.file.Files.createDirectories(outputDir);
            java.nio.file.Files.writeString(outputDir.resolve("orders.json"),
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(OrderMapper.toOrders(items, currencies)),
                    StandardCharsets.UTF_8);
            java.nio.file.Files.writeString(outputDir.resolve("report.csv"), OrderMapper.toReportCsv(items),
                    StandardCharsets.UTF_8);
            java.nio.file.Files.deleteIfExists(input.toPath());
        } catch (Exception e) {
            log.error("Failed to process orders file {}", input, e);
        }
    }

    /** Mule http:request-config host=localhost port=${http.port} basePath=/api/: the app's own currency API. */
    private String currencyBaseUrl() {
        String configured = environment.getProperty("orders.currency-base-url");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return "http://localhost:" + environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8081"));
    }
}
