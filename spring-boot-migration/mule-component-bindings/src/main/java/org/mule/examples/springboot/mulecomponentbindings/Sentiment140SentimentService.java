package org.mule.examples.springboot.mulecomponentbindings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Sentiment140SentimentService: POST bulkClassifyJson; polarity 0 = negative, 2 = neutral, otherwise positive. */
@Component
public class Sentiment140SentimentService implements Services.SentimentService {

    private final RestClient rest;
    private final String appId;

    public Sentiment140SentimentService(RestClient.Builder builder,
                                        @Value("${stockstats.sentiment140.base-url}") String baseUrl,
                                        @Value("${stockstats.sentiment140.app-id:}") String appId) {
        this.rest = builder.baseUrl(baseUrl).build();
        this.appId = appId;
    }

    record ClassifyRequest(String id, String text) {
    }

    record BulkClassifyRequest(List<ClassifyRequest> data) {
    }

    @Override
    public void classify(List<Model.Tweet> tweets) {
        Map<String, Model.Tweet> idToTweet = new HashMap<>();
        List<ClassifyRequest> data = tweets.stream().map(t -> {
            idToTweet.put(t.getId(), t);
            return new ClassifyRequest(t.getId(), t.getText());
        }).toList();
        Object[] result = rest.post().uri("/api/bulkClassifyJson").contentType(MediaType.APPLICATION_JSON)
                .header("appid", appId).body(new BulkClassifyRequest(data))
                .exchange((req, res) -> new Object[] {res.getStatusCode().value(),
                        res.getStatusCode().value() == 200 ? res.bodyTo(JsonNode.class) : null});
        if ((int) result[0] != 200) {
            throw new RuntimeException("Sentiment140 returned status " + result[0]);
        }
        for (JsonNode node : ((JsonNode) result[1]).path("data")) {
            int polarity = node.path("polarity").asInt();
            Model.Sentiment sentiment = polarity == 0 ? Model.Sentiment.NEGATIVE
                    : polarity == 2 ? Model.Sentiment.NEUTRAL : Model.Sentiment.POSITIVE;
            idToTweet.get(node.path("id").asText()).setSentiment(sentiment);
        }
    }
}
