package org.mule.examples.springboot.parity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Loads {@link ParityFixture}s from {@code classpath:<dir>/*.json}, resolving {@code bodyFile} references. */
public final class ParityFixtures {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private ParityFixtures() {
    }

    public static List<ParityFixture> load(String classpathDir) {
        String dir = classpathDir.replaceAll("^/+|/+$", "");
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:" + dir + "/*.json");
            if (resources.length == 0) {
                throw new IllegalStateException("No parity fixtures found under classpath:" + dir);
            }
            List<ParityFixture> fixtures = new ArrayList<>();
            for (Resource resource : Arrays.stream(resources)
                    .sorted(Comparator.comparing(Resource::getFilename)).toList()) {
                ParityFixture raw = MAPPER.readValue(resource.getInputStream(), ParityFixture.class);
                fixtures.add(resolve(raw, resource));
            }
            return fixtures;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load parity fixtures from " + dir, e);
        }
    }

    /** Body content as sent/compared: textual nodes verbatim, other JSON compactly serialized. */
    public static String bodyText(JsonNode body) {
        if (body == null || body.isNull() || body.isMissingNode()) {
            return null;
        }
        return body.isTextual() ? body.asText() : body.toString();
    }

    private static ParityFixture resolve(ParityFixture f, Resource fixtureFile) throws IOException {
        if (f.name() == null || f.request() == null || f.expected() == null) {
            throw new IllegalStateException(fixtureFile.getFilename() + ": 'name', 'request' and 'expected' are required");
        }
        ParityFixture.Request r = f.request();
        ParityFixture.Request request = new ParityFixture.Request(r.method(), r.path(), r.headers(),
                file(fixtureFile, r.bodyFile(), r.body()), null);
        ParityFixture.Expected e = f.expected();
        ParityFixture.Expected expected = new ParityFixture.Expected(e.status(), e.headers(), e.bodyFormat(),
                file(fixtureFile, e.bodyFile(), e.body()), null, e.jsonCompareMode(), e.ignorePaths());
        List<ParityFixture.DownstreamStub> downstream = new ArrayList<>();
        for (ParityFixture.DownstreamStub d : f.downstream()) {
            downstream.add(new ParityFixture.DownstreamStub(d.method(), d.urlPath(), d.queryParams(), d.status(),
                    d.headers(), file(fixtureFile, d.bodyFile(), d.body()), null));
        }
        List<ParityFixture.OutboundExpectation> outbound = new ArrayList<>();
        for (ParityFixture.OutboundExpectation o : f.expectedOutbound()) {
            outbound.add(new ParityFixture.OutboundExpectation(o.method(), o.urlPath(), o.queryParams(), o.headers(),
                    o.bodyFormat(), file(fixtureFile, o.bodyFile(), o.body()), null, o.count(), o.jsonCompareMode(),
                    o.ignorePaths()));
        }
        return new ParityFixture(f.name(), f.description(), f.source(), request, expected, downstream, outbound);
    }

    private static JsonNode file(Resource fixtureFile, String bodyFile, JsonNode inline) throws IOException {
        if (bodyFile == null) {
            return inline;
        }
        Resource body = fixtureFile.createRelative(bodyFile);
        return TextNode.valueOf(new String(body.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }
}
