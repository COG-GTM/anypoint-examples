package org.mule.examples.springboot.proxyingasoapapi;

import org.springframework.http.HttpHeaders;

import java.util.Locale;
import java.util.Set;

/**
 * The Mule {@code copy-headers} sub-flow: copy every property, then remove {@code Host},
 * {@code Content-Length}, {@code http.*}, {@code MULE_*}, {@code Connection} and
 * {@code transfer-encoding}. Applied to the request going to the API and to the response going back.
 * The HTTP/2 cleartext upgrade headers ({@code Upgrade}, {@code HTTP2-Settings}) are also dropped:
 * they only make sense together with the removed {@code Connection} header (see README deltas).
 */
final class MuleHeaderFilter {

    private static final Set<String> REMOVED = Set.of("host", "content-length", "connection", "transfer-encoding", "upgrade", "http2-settings");

    private MuleHeaderFilter() {
    }

    static boolean isCopied(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return !REMOVED.contains(n) && !n.startsWith("http.") && !n.startsWith("mule_");
    }

    static HttpHeaders copy(HttpHeaders source) {
        HttpHeaders target = new HttpHeaders();
        source.forEach((name, values) -> {
            if (isCopied(name)) {
                target.addAll(name, values);
            }
        });
        return target;
    }
}
