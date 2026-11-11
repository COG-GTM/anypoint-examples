package org.mule.examples.springboot.extractingdatafromldapdirectory;

import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.BodyFormat;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.ParityComparator;
import org.mule.examples.springboot.parity.ParityFixture;
import org.mule.examples.springboot.parity.ParityFixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP parity against an embedded LDAP server. Status and headers are compared by the shared comparator. The body
 * (the README's list of LDAP entries) is compared as a set of entries, each with its set of attribute values: neither
 * LDAP nor the Mule connector guarantee entry or attribute order (see README "Behavioural deltas"). The list shape,
 * {@code [dn: ...\n...\n\n, dn: ...\n\n]}, is checked as well.
 */
class LdapParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/extracting-data-from-LDAP-directory";
    }

    @Override
    protected void runCase(ParityFixture fixture) {
        CapturedResponse spring = http.send(springBaseUrl(), fixture.request());
        assertMatches(fixture, "spring-boot", spring);
        Optional<String> mule = muleBaseUrl();
        if (mule.isPresent()) {
            CapturedResponse muleResponse = http.send(mule.get(), fixture.request());
            assertMatches(fixture, "mule", muleResponse);
            assertThat(LdapEntries.canonical(muleResponse.bodyAsString()))
                    .as("[%s] mule vs spring-boot entries", fixture.name())
                    .isEqualTo(LdapEntries.canonical(spring.bodyAsString()));
        }
    }

    private static void assertMatches(ParityFixture fixture, String side, CapturedResponse actual) {
        ParityFixture.Expected e = fixture.expected();
        ParityFixture.Expected statusAndHeaders = new ParityFixture.Expected(e.status(), e.headers(), BodyFormat.IGNORE,
                null, null, null, null);
        assertThat(ParityComparator.compare(statusAndHeaders, actual)).as("[%s] %s status/headers", fixture.name(), side)
                .isEmpty();
        String body = actual.bodyAsString();
        assertThat(body).as("[%s] %s list shape", fixture.name(), side).matches("(?s)\\[(dn: .*\\n\\n(, (?=dn: ))?)*\\]");
        assertThat(LdapEntries.canonical(body)).as("[%s] %s entries", fixture.name(), side)
                .isEqualTo(LdapEntries.canonical(ParityFixtures.bodyText(e.body())));
    }

    /** Parses the {@code [dn: ...]} text into dn -> sorted attribute lines. */
    static final class LdapEntries {

        static Map<String, List<String>> canonical(String text) {
            String inner = text.trim();
            inner = inner.substring(1, inner.length() - 1);
            Map<String, List<String>> entries = new TreeMap<>();
            for (String block : inner.split("\\n\\n(, )?")) {
                if (block.isBlank()) {
                    continue;
                }
                List<String> lines = new ArrayList<>(List.of(block.strip().split("\\n")));
                String dn = lines.remove(0);
                lines.sort(null);
                entries.put(dn, lines);
            }
            return entries;
        }
    }
}
