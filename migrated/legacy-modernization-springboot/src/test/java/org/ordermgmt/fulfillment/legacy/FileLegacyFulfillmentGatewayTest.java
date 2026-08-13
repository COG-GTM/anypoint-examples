package org.ordermgmt.fulfillment.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLegacyFulfillmentGatewayTest {

    @Test
    void writesOneTimestampedFilePerOrderUsingTheMuleOutputPattern(@TempDir Path tempDir) throws IOException {
        LegacyFulfillmentProperties properties = new LegacyFulfillmentProperties();
        properties.setOutputDirectory(tempDir.resolve("Output").toString());
        Clock clock = Clock.fixed(Instant.parse("2024-03-05T10:15:30.123Z"), ZoneId.of("UTC"));

        new FileLegacyFulfillmentGateway(properties, clock).send("MSKU,QTY\n1234,500\n");

        try (var files = Files.list(tempDir.resolve("Output"))) {
            List<Path> written = files.toList();
            assertThat(written).hasSize(1);
            assertThat(written.get(0).getFileName()).hasToString("ShippingOrder-05-03-24_10-15-30.123.txt");
            assertThat(Files.readString(written.get(0))).isEqualTo("MSKU,QTY\n1234,500\n");
        }
    }
}
