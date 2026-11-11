package org.mule.examples.springboot.parity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/** Loads {@link MessagingParityFixture}s from {@code classpath:<dir>/*.json}, resolving {@code bodyFile} references. */
public final class MessagingParityFixtures {

    private MessagingParityFixtures() {
    }

    public static List<MessagingParityFixture> load(String classpathDir) {
        String dir = classpathDir.replaceAll("^/+|/+$", "");
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:" + dir + "/*.json");
            if (resources.length == 0) {
                throw new IllegalStateException("No messaging parity fixtures found under classpath:" + dir);
            }
            List<MessagingParityFixture> fixtures = new ArrayList<>();
            for (Resource resource : Arrays.stream(resources)
                    .sorted(Comparator.comparing(Resource::getFilename)).toList()) {
                MessagingParityFixture raw = ParityFixtures.MAPPER.readValue(resource.getInputStream(), MessagingParityFixture.class);
                if (raw.name() == null) {
                    throw new IllegalStateException(resource.getFilename() + ": 'name' is required");
                }
                fixtures.add(resolve(raw, resource));
            }
            return fixtures;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load messaging parity fixtures from " + dir, e);
        }
    }

    /** The messaging part of the fixture called {@code name} (e.g. to pair with an HTTP {@link ParityFixture}). */
    public static MessagingParityFixture byName(String classpathDir, String name) {
        return load(classpathDir).stream().filter(f -> f.name().equals(name)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No fixture named " + name + " under " + classpathDir));
    }

    private static MessagingParityFixture resolve(MessagingParityFixture f, Resource file) throws IOException {
        List<MessagingParityFixture.OutboundMessage> send = new ArrayList<>();
        for (MessagingParityFixture.OutboundMessage m : f.send()) {
            send.add(new MessagingParityFixture.OutboundMessage(m.destination(), body(file, m.bodyFile(), m.body()),
                    null, m.properties()));
        }
        List<MessagingParityFixture.MessageExpectation> expected = new ArrayList<>();
        for (MessagingParityFixture.MessageExpectation e : f.expectedMessages()) {
            expected.add(new MessagingParityFixture.MessageExpectation(e.destination(), e.count(), e.type(),
                    e.bodyFormat(), body(file, e.bodyFile(), e.body()), null, e.jsonCompareMode(), e.ignorePaths(),
                    e.properties()));
        }
        return new MessagingParityFixture(f.name(), f.description(), f.source(), send, expected, f.expectedState());
    }

    private static JsonNode body(Resource fixtureFile, String bodyFile, JsonNode inline) throws IOException {
        if (bodyFile == null) {
            return inline;
        }
        return TextNode.valueOf(new String(fixtureFile.createRelative(bodyFile).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
    }
}
