package org.mule.examples.springboot.filteringamessage;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@code http:listener path="/" allowedMethods="POST"} with {@code http:response-builder statusCode="200"}.
 * The body is parsed regardless of Content-Type, like {@code json:json-to-object-transformer}.
 */
@RestController
public class FilteringController {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final DiscountGateway gateway;
    private final ObjectMapper mapper;

    public FilteringController(DiscountGateway gateway, ObjectMapper mapper) {
        this.gateway = gateway;
        this.mapper = mapper;
    }

    @PostMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> filter(@RequestBody(required = false) byte[] body) throws Exception {
        // and-filter (payload-type BufferInputStream AND http.method=post): a request without a body stream is dropped.
        if (body == null || body.length == 0) {
            return ResponseEntity.ok().build();
        }
        String outcome = gateway.evaluate(mapper.readValue(body, MAP));
        return outcome.isEmpty() ? ResponseEntity.ok().build() : ResponseEntity.ok(outcome);
    }
}
