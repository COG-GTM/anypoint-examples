package org.mule.examples.springboot.parity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A response captured from either the Mule runtime or the Spring Boot app. */
public record CapturedResponse(int status, Map<String, List<String>> headers, byte[] body) {

    public String bodyAsString() {
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    public Optional<String> header(String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(name))
                .flatMap(e -> e.getValue().stream())
                .findFirst();
    }
}
