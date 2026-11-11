package org.mule.examples.springboot.parity;

import org.json.JSONException;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Compares captured responses/bodies and returns human-readable mismatches (empty list = parity). */
public final class ParityComparator {

    public static final String ABSENT = "!absent";

    private ParityComparator() {
    }

    /** Compares a captured response against a fixture's expected response. */
    public static List<String> compare(ParityFixture.Expected expected, CapturedResponse actual) {
        List<String> mismatches = new ArrayList<>();
        if (expected.status() != null && expected.status() != actual.status()) {
            mismatches.add("status: expected " + expected.status() + " but was " + actual.status());
        }
        mismatches.addAll(compareHeaders(expected.headers(), actual));
        mismatches.addAll(compareBody(expected.bodyFormat(), ParityFixtures.bodyText(expected.body()),
                actual.bodyAsString(), expected.jsonCompareMode(), expected.ignorePaths()));
        return mismatches;
    }

    /**
     * Compares two captured responses directly (Mule vs Spring Boot): status, the headers named
     * in the fixture (must be equal, unless the fixture value is a {@code ~regex} both match), and
     * the body using the fixture's format.
     */
    public static List<String> compareResponses(ParityFixture.Expected expected, CapturedResponse mule, CapturedResponse spring) {
        List<String> mismatches = new ArrayList<>();
        if (mule.status() != spring.status()) {
            mismatches.add("status: mule " + mule.status() + " vs spring " + spring.status());
        }
        for (Map.Entry<String, String> header : expected.headers().entrySet()) {
            String name = header.getKey();
            Optional<String> m = mule.header(name);
            Optional<String> s = spring.header(name);
            boolean tolerated = header.getValue().startsWith("~")
                    && m.isPresent() && s.isPresent()
                    && textMatches(header.getValue(), m.get()) && textMatches(header.getValue(), s.get());
            if (!tolerated && !m.equals(s)) {
                mismatches.add("header " + name + ": mule " + m.orElse(ABSENT) + " vs spring " + s.orElse(ABSENT));
            }
        }
        BodyFormat format = expected.bodyFormat();
        if (format == BodyFormat.TEXT || format == BodyFormat.NONE) {
            // captured Mule text is literal, never a ~regex
            String m = mule.bodyAsString() == null ? "" : mule.bodyAsString();
            String s = spring.bodyAsString() == null ? "" : spring.bodyAsString();
            if (!m.equals(s)) {
                mismatches.add("body: mule '" + abbreviate(m) + "' vs spring '" + abbreviate(s) + "'");
            }
        } else if (format != BodyFormat.IGNORE) {
            mismatches.addAll(compareBody(format, mule.bodyAsString(), spring.bodyAsString(),
                    expected.jsonCompareMode(), expected.ignorePaths()));
        }
        return mismatches;
    }

    public static List<String> compareHeaders(Map<String, String> expected, CapturedResponse actual) {
        List<String> mismatches = new ArrayList<>();
        expected.forEach((name, want) -> {
            Optional<String> got = actual.header(name);
            if (ABSENT.equals(want)) {
                got.ifPresent(v -> mismatches.add("header " + name + ": expected absent but was '" + v + "'"));
            } else if (got.isEmpty()) {
                mismatches.add("header " + name + ": expected '" + want + "' but was absent");
            } else if (!textMatches(want, got.get())) {
                mismatches.add("header " + name + ": expected '" + want + "' but was '" + got.get() + "'");
            }
        });
        return mismatches;
    }

    public static List<String> compareBody(BodyFormat format, String expected, String actual,
                                           String jsonCompareMode, List<String> ignorePaths) {
        String exp = expected == null ? "" : expected;
        String act = actual == null ? "" : actual;
        return switch (format) {
            case IGNORE -> List.of();
            case NONE -> act.isEmpty() ? List.of() : List.of("body: expected empty but was '" + abbreviate(act) + "'");
            case TEXT -> textMatches(exp, act) ? List.of()
                    : List.of("body: expected '" + abbreviate(exp) + "' but was '" + abbreviate(act) + "'");
            case JSON -> compareJson(exp, act, jsonCompareMode, ignorePaths);
            case XML -> compareXml(exp, act);
        };
    }

    static boolean textMatches(String expected, String actual) {
        if (expected.startsWith("~")) {
            return actual.matches("(?s)" + expected.substring(1));
        }
        return expected.equals(actual);
    }

    private static List<String> compareJson(String expected, String actual, String mode, List<String> ignorePaths) {
        try {
            JSONCompareMode compareMode = JSONCompareMode.valueOf(mode);
            Customization[] customizations = ignorePaths.stream()
                    .map(p -> new Customization(p, (o1, o2) -> true))
                    .toArray(Customization[]::new);
            JSONCompareResult result = JSONCompare.compareJSON(expected, actual, new CustomComparator(compareMode, customizations));
            return result.failed() ? List.of("json body: " + result.getMessage().trim().replace('\n', ' ')) : List.of();
        } catch (JSONException | IllegalArgumentException e) {
            return List.of("json body: not comparable (" + e.getMessage() + "); expected '" + abbreviate(expected)
                    + "' but was '" + abbreviate(actual) + "'");
        }
    }

    /**
     * A fixture text node of the form {@code ~regex} matches any actual text the regex fully matches, for values the
     * Mule app generates (timestamps, ids). Plain text is still compared by XMLUnit similarity.
     */
    private static ComparisonResult regexText(org.xmlunit.diff.Comparison comparison, ComparisonResult outcome) {
        if (outcome == ComparisonResult.EQUAL || comparison.getType() != ComparisonType.TEXT_VALUE) {
            return outcome;
        }
        Object control = comparison.getControlDetails().getValue();
        Object test = comparison.getTestDetails().getValue();
        if (control instanceof String c && c.strip().startsWith("~") && test instanceof String t
                && t.strip().matches(c.strip().substring(1))) {
            return ComparisonResult.EQUAL;
        }
        return outcome;
    }

    private static List<String> compareXml(String expected, String actual) {
        try {
            Diff diff = DiffBuilder.compare(expected).withTest(actual)
                    .ignoreWhitespace()
                    .ignoreComments()
                    .normalizeWhitespace()
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText, ElementSelectors.byName))
                    .withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default, ParityComparator::regexText))
                    .checkForSimilar()
                    .build();
            List<String> mismatches = new ArrayList<>();
            diff.getDifferences().forEach(d -> mismatches.add("xml body: " + d.getComparison().toString()));
            return mismatches;
        } catch (RuntimeException e) {
            return List.of("xml body: not comparable (" + e.getMessage() + ")");
        }
    }

    private static String abbreviate(String s) {
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
