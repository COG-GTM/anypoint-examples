package org.ordermgmt.fulfillment.legacy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Replaces the {@code file:outbound-endpoint} "LegacyFulfillment". Writes one file per order using
 * the Mule output pattern
 * {@code ShippingOrder-#[server.dateTime.format('dd-MM-yy_HH-mm-ss.SSS')].txt}.
 */
@Component
public class FileLegacyFulfillmentGateway implements LegacyFulfillmentGateway {

    private static final Logger log = LoggerFactory.getLogger(FileLegacyFulfillmentGateway.class);
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("dd-MM-yy_HH-mm-ss.SSS");

    private final Path outputDirectory;
    private final Clock clock;

    public FileLegacyFulfillmentGateway(LegacyFulfillmentProperties properties, Clock clock) {
        this.outputDirectory = Path.of(properties.getOutputDirectory());
        this.clock = clock;
    }

    @Override
    public void send(String csvPayload) {
        Path file = outputDirectory.resolve("ShippingOrder-" + LocalDateTime.now(clock).format(FILE_TIMESTAMP) + ".txt");
        try {
            Files.createDirectories(outputDirectory);
            Files.writeString(file, csvPayload, StandardCharsets.UTF_8);
            log.info("Wrote legacy fulfillment order to {}", file);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write legacy fulfillment order to " + file, e);
        }
    }
}
