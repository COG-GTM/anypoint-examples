package org.mule.examples.springboot.legacymodernization;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.mule.examples.springboot.legacymodernization.model.ShippingOrderConfirmation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * The {@code async} scope: DataWeave to CSV, then {@code file:outbound-endpoint path="src/main/resources/Output"
 * outputPattern="ShippingOrder-#[server.dateTime.format('dd-MM-yy_HH-mm-ss.SSS')].txt"}.
 */
@Component
public class LegacyFulfillmentFileWriter {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd-MM-yy_HH-mm-ss.SSS");

    private final Path outputPath;

    public LegacyFulfillmentFileWriter(@Value("${legacy.output.path}") Path outputPath) {
        this.outputPath = outputPath;
    }

    @Async
    public void write(ShippingOrderConfirmation confirmation) {
        try {
            Files.createDirectories(outputPath);
            Path file = outputPath.resolve("ShippingOrder-" + LocalDateTime.now().format(TIMESTAMP) + ".txt");
            Files.writeString(file, LegacyFulfillmentCsv.render(confirmation), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
