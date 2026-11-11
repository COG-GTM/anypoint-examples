package org.mule.examples.springboot.parity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/** Compares messages captured from a broker with a {@link MessagingParityFixture.MessageExpectation}. */
public final class MessagingParityAssertions {

    /** A message read from a queue/topic/stream by the test. */
    public record CapturedMessage(String type, String body, Map<String, String> properties) {
        public CapturedMessage {
            properties = properties == null ? Map.of() : Map.copyOf(properties);
        }
    }

    private MessagingParityAssertions() {
    }

    public static List<String> compare(MessagingParityFixture.MessageExpectation expected, List<CapturedMessage> actual) {
        List<String> mismatches = new ArrayList<>();
        String where = expected.destination();
        if (actual.size() != expected.count()) {
            mismatches.add(where + ": expected " + expected.count() + " message(s) but captured " + actual.size());
            return mismatches;
        }
        for (int i = 0; i < actual.size(); i++) {
            CapturedMessage m = actual.get(i);
            String prefix = where + "[" + i + "] ";
            if (expected.type() != null && !expected.type().equals(m.type())) {
                mismatches.add(prefix + "type: expected " + expected.type() + " but was " + m.type());
            }
            expected.properties().forEach((name, want) -> {
                String got = m.properties().get(name);
                if (ParityComparator.ABSENT.equals(want)) {
                    if (got != null) {
                        mismatches.add(prefix + "property " + name + ": expected absent but was '" + got + "'");
                    }
                } else if (got == null || !ParityComparator.textMatches(want, got)) {
                    mismatches.add(prefix + "property " + name + ": expected '" + want + "' but was '" + got + "'");
                }
            });
            ParityComparator.compareBody(expected.bodyFormat(), expected.bodyText(), m.body(),
                    expected.jsonCompareMode(), expected.ignorePaths()).forEach(s -> mismatches.add(prefix + s));
        }
        return mismatches;
    }

    public static void assertMessages(MessagingParityFixture fixture, MessagingParityFixture.MessageExpectation expected,
                                      List<CapturedMessage> actual) {
        List<String> mismatches = compare(expected, actual);
        if (!mismatches.isEmpty()) {
            fail("[" + fixture.name() + "] messaging parity failed:\n  - " + String.join("\n  - ", mismatches));
        }
    }
}
