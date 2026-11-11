package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;

/** Isolated input/output directories so tests never touch src/main/resources/{input,output}. */
final class OrdersTestDirectories {

    static final Path ROOT = create();
    static final Path INPUT = ROOT.resolve("input");
    static final Path OUTPUT = ROOT.resolve("output");

    private OrdersTestDirectories() {
    }

    static void register(DynamicPropertyRegistry registry) {
        registry.add("orders.input-dir", INPUT::toString);
        registry.add("orders.output-dir", OUTPUT::toString);
    }

    private static Path create() {
        try {
            return Files.createTempDirectory("processing-orders-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
