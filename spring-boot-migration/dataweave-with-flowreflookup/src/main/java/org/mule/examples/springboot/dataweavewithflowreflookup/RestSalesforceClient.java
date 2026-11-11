package org.mule.examples.springboot.dataweavewithflowreflookup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Salesforce REST implementation: {@code POST /services/data/{version}/composite/sobjects} creates all rows in one
 * call, like the connector's batched {@code create}. Authentication is a pre-issued OAuth access token.
 */
@Component
public class RestSalesforceClient implements SalesforceClient {

    private static final Logger log = LoggerFactory.getLogger(RestSalesforceClient.class);

    private final RestClient rest;
    private final String apiVersion;

    public RestSalesforceClient(RestClient.Builder builder,
                                @Value("${salesforce.base-url}") String baseUrl,
                                @Value("${salesforce.access-token:}") String accessToken,
                                @Value("${salesforce.api-version:v59.0}") String apiVersion) {
        this.rest = builder.baseUrl(baseUrl).defaultHeader("Authorization", "Bearer " + accessToken).build();
        this.apiVersion = apiVersion;
    }

    @Override
    public void create(String type, List<Map<String, Object>> objects) {
        List<Map<String, Object>> records = objects.stream().map(o -> {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("attributes", Map.of("type", type));
            record.putAll(o);
            return record;
        }).toList();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("allOrNone", false);
        request.put("records", records);
        String result = rest.post()
                .uri("/services/data/{version}/composite/sobjects", apiVersion)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
        log.info("Salesforce create {} x{}: {}", type, objects.size(), result);
    }
}
