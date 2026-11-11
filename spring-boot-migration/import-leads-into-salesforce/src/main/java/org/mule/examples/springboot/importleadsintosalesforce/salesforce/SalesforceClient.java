package org.mule.examples.springboot.importleadsintosalesforce.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Typed Salesforce REST API client standing in for the Mule Salesforce connector operations used by
 * this app. Queries follow {@code nextRecordsUrl} like the connector's paging iterator; creates use
 * sObject Collections (up to 200 records per call, {@code allOrNone=false}, i.e. per-record results
 * like the SOAP {@code create} call).
 */
public class SalesforceClient {

    public static final int MAX_RECORDS_PER_CALL = 200;

    private final RestClient http;
    private final SalesforceTokenProvider tokens;
    private final String apiVersion;

    public SalesforceClient(RestClient http, SalesforceTokenProvider tokens, String apiVersion) {
        this.http = http;
        this.tokens = tokens;
        this.apiVersion = apiVersion;
    }

    /** {@code sfdc:query}. */
    public List<Map<String, Object>> query(String soql) {
        return pagedQuery("query", soql);
    }

    /** {@code sfdc:query-all} (includes deleted and archived records). */
    public List<Map<String, Object>> queryAll(String soql) {
        return pagedQuery("queryAll", soql);
    }

    /** {@code sfdc:describe-global}. */
    public JsonNode describeGlobal() {
        return call(t -> http.get().uri(dataUrl(t) + "/sobjects").headers(h -> h.setBearerAuth(t.accessToken())).retrieve().body(JsonNode.class));
    }

    /** {@code sfdc:create type=...}: one result per record, in input order. */
    public List<SaveResult> create(String type, List<Map<String, Object>> records) {
        List<SaveResult> results = new ArrayList<>();
        for (int from = 0; from < records.size(); from += MAX_RECORDS_PER_CALL) {
            List<Map<String, Object>> chunk = records.subList(from, Math.min(records.size(), from + MAX_RECORDS_PER_CALL));
            List<Map<String, Object>> body = new ArrayList<>();
            for (Map<String, Object> record : chunk) {
                Map<String, Object> sobject = new LinkedHashMap<>();
                sobject.put("attributes", Map.of("type", type));
                sobject.putAll(record);
                body.add(sobject);
            }
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("allOrNone", false);
            request.put("records", body);
            JsonNode response = call(t -> http.post().uri(dataUrl(t) + "/composite/sobjects")
                    .headers(h -> h.setBearerAuth(t.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class));
            response.forEach(r -> results.add(new SaveResult(
                    r.path("id").isNull() ? null : r.path("id").asText(null),
                    r.path("success").asBoolean(),
                    r.path("errors").toString())));
        }
        return results;
    }

    private List<Map<String, Object>> pagedQuery(String resource, String soql) {
        List<Map<String, Object>> records = new ArrayList<>();
        JsonNode page = call(t -> http.get().uri(dataUrl(t) + "/" + resource + "?q={q}", soql)
                .headers(h -> h.setBearerAuth(t.accessToken())).retrieve().body(JsonNode.class));
        records.addAll(SalesforceRecords.records(page.path("records")));
        while (!page.path("done").asBoolean(true) && page.hasNonNull("nextRecordsUrl")) {
            String next = page.path("nextRecordsUrl").asText();
            page = call(t -> http.get().uri(URI.create(t.instanceUrl() + next))
                    .headers(h -> h.setBearerAuth(t.accessToken())).retrieve().body(JsonNode.class));
            records.addAll(SalesforceRecords.records(page.path("records")));
        }
        return records;
    }

    private String dataUrl(SalesforceToken token) {
        return token.instanceUrl() + "/services/data/v" + apiVersion;
    }

    private <T> T call(Function<SalesforceToken, T> request) {
        SalesforceToken token = tokens.current();
        try {
            return request.apply(token);
        } catch (HttpClientErrorException.Unauthorized expired) {
            return request.apply(tokens.renew(token));
        }
    }

    /** Outcome of one record in a create call. */
    public record SaveResult(String id, boolean success, String errors) {
    }
}
