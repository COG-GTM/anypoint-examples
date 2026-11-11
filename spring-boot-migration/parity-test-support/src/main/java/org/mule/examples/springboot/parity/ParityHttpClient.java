package org.mule.examples.springboot.parity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Replays a fixture request against any base URL. Redirects are NOT followed so 3xx responses
 * (login forms, OAuth redirects) are compared as-is.
 */
public class ParityHttpClient {

    private static final Set<String> RESTRICTED_HEADERS = Set.of("connection", "content-length", "host", "expect", "upgrade");

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CapturedResponse send(String baseUrl, ParityFixture.Request request) {
        String body = ParityFixtures.bodyText(request.body());
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(stripSlash(baseUrl) + request.path()))
                .timeout(Duration.ofSeconds(30))
                .method(request.method(), publisher);
        request.headers().forEach((k, v) -> {
            if (!RESTRICTED_HEADERS.contains(k.toLowerCase())) {
                builder.header(k, v);
            }
        });
        try {
            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new CapturedResponse(response.statusCode(), response.headers().map(), response.body());
        } catch (IOException e) {
            throw new UncheckedIOException("Request to " + baseUrl + request.path() + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
