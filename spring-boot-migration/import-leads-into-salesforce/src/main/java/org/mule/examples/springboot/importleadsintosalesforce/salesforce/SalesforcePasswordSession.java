package org.mule.examples.springboot.importleadsintosalesforce.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Replaces the Mule {@code sfdc:config username/password/securityToken} connection: logs in with the
 * OAuth2 username-password flow ({@code password} = password + security token, as in the SOAP login
 * the Mule connector performed) and keeps the session until the API rejects it.
 */
public class SalesforcePasswordSession implements SalesforceTokenProvider {

    private final RestClient http;
    private final String loginUrl;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;
    private final String securityToken;
    private SalesforceToken token;

    public SalesforcePasswordSession(RestClient http, String loginUrl, String clientId, String clientSecret,
                                     String username, String password, String securityToken) {
        this.http = http;
        this.loginUrl = loginUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
        this.securityToken = securityToken;
    }

    @Override
    public synchronized SalesforceToken current() {
        if (token == null) {
            token = login();
        }
        return token;
    }

    @Override
    public synchronized SalesforceToken renew(SalesforceToken stale) {
        if (token == null || token.equals(stale)) {
            token = login();
        }
        return token;
    }

    /** Drops the cached session so the next call logs in again. */
    public synchronized void invalidate() {
        token = null;
    }

    private SalesforceToken login() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", username);
        form.add("password", password + nullToEmpty(securityToken));
        JsonNode body = http.post()
                .uri(loginUrl + "/services/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        return new SalesforceToken(body.path("access_token").asText(), body.path("instance_url").asText());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
