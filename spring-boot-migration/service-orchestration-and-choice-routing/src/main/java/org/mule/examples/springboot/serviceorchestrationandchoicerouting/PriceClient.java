package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The {@code ee:cache} (Caching_Strategy, key {@code #[payload.productId]}, in-memory store) around http:request GET
 * {@code api/prices/#[payload.productId]} + object-to-string: Spring Cache {@code prices} keyed by product id.
 */
@Component
public class PriceClient {

    private final RestClient rest;

    private final Environment env;

    /** The base URL is resolved per call so it can point at this application's own (random) port. */
    public PriceClient(RestClient.Builder builder, Environment env) {
        this.rest = builder.build();
        this.env = env;
    }

    @Cacheable(cacheNames = "prices", key = "#productId")
    public String price(String productId) {
        return rest.get().uri(env.getRequiredProperty("price.base-url") + "/api/prices/{productId}", productId).retrieve().body(String.class);
    }
}
