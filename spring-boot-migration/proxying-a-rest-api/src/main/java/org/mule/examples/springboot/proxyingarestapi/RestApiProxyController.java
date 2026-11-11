package org.mule.examples.springboot.proxyingarestapi;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.Optional;

/**
 * Mule flow {@code rest-api-proxy}: {@code http:listener path="/*"} → {@code copy-headers} →
 * {@code http:request method=#[http.method] path=#[http.request.path]} with the inbound query params
 * ({@code parseResponse="false"}) → {@code copy-headers}.
 */
@RestController
class RestApiProxyController {

    private static final Logger log = LoggerFactory.getLogger(RestApiProxyController.class);

    private final WebClient api;

    RestApiProxyController(WebClient proxyWebClient) {
        this.api = proxyWebClient;
    }

    @RequestMapping("/**")
    ResponseEntity<byte[]> proxy(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        HttpHeaders outbound = new HttpHeaders();
        for (String name : Collections.list(request.getHeaderNames())) {
            if (MuleHeaderFilter.isCopied(name)) {
                outbound.addAll(name, Collections.list(request.getHeaders(name)));
            }
        }
        String uri = request.getRequestURI() + Optional.ofNullable(request.getQueryString()).map(q -> "?" + q).orElse("");

        WebClient.RequestBodySpec spec = api.method(HttpMethod.valueOf(request.getMethod()))
                .uri(uri)
                .headers(h -> h.addAll(outbound));
        if (body != null && body.length > 0) {
            spec.bodyValue(body);
        }
        ResponseEntity<byte[]> upstream = spec.exchangeToMono(r -> r.toEntity(byte[].class)).block();
        if (upstream == null) {
            throw new IllegalStateException("No response from API for " + uri);
        }

        int status = upstream.getStatusCode().value();
        if (status >= 400) {
            // Mule 3.8 http:request default success-status-code-validator: 0..399; anything else fails the flow (500).
            log.error("Response code {} mapped as failure.", status);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Response code " + status + " mapped as failure.").getBytes());
        }
        // copy-headers removes the http.status property, so the listener answers with its default 200.
        return ResponseEntity.ok()
                .headers(MuleHeaderFilter.copy(upstream.getHeaders()))
                .body(upstream.getBody() == null ? new byte[0] : upstream.getBody());
    }
}
