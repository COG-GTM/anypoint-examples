package org.mule.examples.springboot.parity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the response captured from a live Mule runtime as a new fixture (same request, expected
 * block filled from the capture, {@code evidence: recorded}) so it can be reviewed and promoted
 * into {@code src/test/resources/parity/...}.
 */
public final class ParityRecorder {

    private final Path directory;

    public ParityRecorder(Path directory) {
        this.directory = directory;
    }

    public Path record(ParityFixture fixture, CapturedResponse mule) {
        ParityFixture.Expected e = fixture.expected();
        Map<String, String> headers = new LinkedHashMap<>();
        e.headers().keySet().forEach(h -> headers.put(h, mule.header(h).orElse(ParityComparator.ABSENT)));
        JsonNode body = e.bodyFormat() == BodyFormat.JSON ? parseOrText(mule.bodyAsString()) : TextNode.valueOf(mule.bodyAsString());
        ParityFixture.Expected recorded = new ParityFixture.Expected(mule.status(), headers, e.bodyFormat(), body, null,
                e.jsonCompareMode(), e.ignorePaths());
        ParityFixture.Source s = fixture.source();
        ParityFixture.Source source = new ParityFixture.Source(s == null ? null : s.muleApp(), s == null ? null : s.flow(),
                "recorded", s == null ? null : s.reference());
        ParityFixture out = new ParityFixture(fixture.name(), fixture.description(), source, fixture.request(), recorded,
                fixture.downstream(), fixture.expectedOutbound());
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve(fixture.name() + ".json");
            ParityFixtures.MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), out);
            return file;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static JsonNode parseOrText(String s) {
        try {
            return ParityFixtures.MAPPER.readTree(s);
        } catch (IOException ex) {
            return TextNode.valueOf(s);
        }
    }
}
