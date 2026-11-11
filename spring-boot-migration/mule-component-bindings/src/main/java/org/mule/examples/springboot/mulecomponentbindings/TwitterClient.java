package org.mule.examples.springboot.mulecomponentbindings;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Replaces {@code twitter:search} + TwitterResponseProcessor: Twitter v1.1 {@code search/tweets.json} with
 * query/since/until only (the Mule connector call passes no paging parameters).
 */
@Component
public class TwitterClient {

    private final RestClient rest;

    public TwitterClient(RestClient.Builder builder, @Value("${twitter.base-url}") String baseUrl,
                         @Value("${twitter.bearer-token:}") String bearerToken) {
        this.rest = builder.baseUrl(baseUrl).defaultHeader("Authorization", "Bearer " + bearerToken).build();
    }

    public List<Model.Tweet> search(String query, String since, String until) {
        JsonNode result = rest.get()
                .uri(u -> u.path("/1.1/search/tweets.json").queryParam("q", "{q}").queryParam("since", since)
                        .queryParam("until", until).build(query))
                .retrieve().body(JsonNode.class);
        List<Model.Tweet> tweets = new ArrayList<>();
        for (JsonNode status : result.path("statuses")) {
            tweets.add(new Model.Tweet(status.path("id").asText(), status.path("text").asText()));
        }
        return tweets;
    }
}
