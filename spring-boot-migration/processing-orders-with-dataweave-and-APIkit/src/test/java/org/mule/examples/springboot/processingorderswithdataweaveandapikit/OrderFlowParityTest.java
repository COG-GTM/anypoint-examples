package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * File-flow parity for books.xml {@code OrderFlow}, mirroring the Mule test
 * ProcessingOrdersWithDataweaveAndApikitIT: drop src/main/resources/orders.xml into the input directory, then compare
 * output/orders.json and output/report.csv with the Mule expected src/main/resources/orders.json and report.csv
 * (copied verbatim to parity/processing-orders-with-dataweave-and-APIkit/order-flow/). The currency lookup goes over
 * HTTP to the app's own /api/currencies, exactly like Mule's http:request to localhost:${http.port}/api/currencies.
 */
@ActiveProfiles("parity")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowParityTest {

    private static final String FIXTURES = "parity/processing-orders-with-dataweave-and-APIkit/order-flow/";

    @DynamicPropertySource
    static void dirs(DynamicPropertyRegistry registry) {
        OrdersTestDirectories.register(registry);
    }

    @Test
    void ordersFileProducesMuleOrdersJsonAndReportCsv() throws Exception {
        Path ordersJson = OrdersTestDirectories.OUTPUT.resolve("orders.json");
        Path reportCsv = OrdersTestDirectories.OUTPUT.resolve("report.csv");
        Files.deleteIfExists(ordersJson);
        Files.deleteIfExists(reportCsv);
        Files.createDirectories(OrdersTestDirectories.INPUT);
        Path input = OrdersTestDirectories.INPUT.resolve("orders.xml");
        Files.write(input, resource("orders.xml").getBytes(StandardCharsets.UTF_8));

        await().atMost(Duration.ofSeconds(20)).until(() -> Files.exists(reportCsv) && Files.exists(ordersJson)
                && !Files.exists(input));

        JSONAssert.assertEquals(resource("orders.json"), Files.readString(ordersJson), JSONCompareMode.STRICT);
        // DataWeave writes 27.60 / 262.80 with BigDecimal scale; check the literal number text too.
        assertThat(Files.readString(ordersJson).replaceAll("\\s", ""))
                .isEqualTo(resource("orders.json").replaceAll("\\s", ""));
        assertThat(Files.readString(reportCsv).replaceAll("\\s", ""))
                .isEqualTo(resource("report.csv").replaceAll("\\s", ""));
        assertThat(input).as("file:inbound-endpoint deletes the consumed file").doesNotExist();
    }

    private static String resource(String name) throws Exception {
        return new String(new ClassPathResource(FIXTURES + name).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
