package org.mule.examples.springboot.parity;

import java.util.List;

/** JUnit-friendly assertions that report every mismatch for a case at once. */
public final class ParityAssertions {

    private ParityAssertions() {
    }

    /** Asserts {@code actual} (captured from {@code side}, e.g. "spring-boot" or "mule") matches the fixture. */
    public static void assertMatchesFixture(ParityFixture fixture, String side, CapturedResponse actual) {
        fail(fixture, side + " vs fixture", ParityComparator.compare(fixture.expected(), actual), actual);
    }

    /** Asserts the Mule and Spring Boot responses are equivalent to each other. */
    public static void assertEquivalent(ParityFixture fixture, CapturedResponse mule, CapturedResponse spring) {
        fail(fixture, "mule vs spring-boot", ParityComparator.compareResponses(fixture.expected(), mule, spring), spring);
    }

    static void fail(ParityFixture fixture, String what, List<String> mismatches, CapturedResponse actual) {
        if (!mismatches.isEmpty()) {
            throw new AssertionError("Parity mismatch [" + fixture.name() + "] " + what + ":\n  - "
                    + String.join("\n  - ", mismatches)
                    + "\n  actual status=" + actual.status() + " body=" + actual.bodyAsString());
        }
    }
}
