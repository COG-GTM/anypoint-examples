package org.mule.examples.springboot.oauth2clientcredentialsusingthehttpconnector;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Consumer;

/**
 * Mule {@code http:request} semantics: send, and treat any status outside 0..399 as a failure
 * ({@code SuccessStatusCodeValidator}: "Response code N mapped as failure").
 */
@Component
class OutboundHttp {

    private final RestClient rest;

    OutboundHttp(RestClient.Builder builder) {
        this.rest = builder.build();
    }

    record Response(int status, MediaType contentType, byte[] body) {
    }

    Response send(HttpMethod method, String url, Consumer<HttpHeaders> headers, byte[] body) {
        RestClient.RequestBodySpec spec = rest.method(method).uri(java.net.URI.create(url)).headers(headers);
        if (body != null && body.length > 0) {
            spec.body(body);
        }
        return spec.exchange((req, res) -> new Response(res.getStatusCode().value(),
                res.getHeaders().getContentType(), res.getBody().readAllBytes()));
    }

    static ResponseEntity<byte[]> toListenerResponse(Response r) {
        if (r.status() >= 400) {
            throw new MuleFlowException("Response code " + r.status() + " mapped as failure.");
        }
        ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
        if (r.contentType() != null) {
            ok.contentType(r.contentType());
        }
        return ok.body(r.body());
    }
}
