package org.mule.examples.springboot.authenticatingsalesforceusingoauth2;

import org.mule.examples.springboot.authenticatingsalesforceusingoauth2.salesforce.SalesforceToken;
import org.mule.examples.springboot.authenticatingsalesforceusingoauth2.salesforce.SalesforceTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Mule {@code sfdc:config-with-oauth} global element plus {@code sfdc:authorize}: OAuth2 web-server
 * (authorization code) flow with the Connected App's consumer key/secret, an in-memory token store
 * (the connector's default object store) and automatic refresh when the access token expires.
 */
@Component
class SalesforceOAuth implements SalesforceTokenProvider {

    private final RestClient http;
    private final String consumerKey;
    private final String consumerSecret;
    private final String authorizationUrl;
    private final String accessTokenUrl;
    private final String display;
    private final String redirectUri;
    static final Duration STATE_TTL = Duration.ofMinutes(10);
    static final int MAX_PENDING_STATES = 1000;

    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();
    private volatile SalesforceToken token;

    SalesforceOAuth(RestClient.Builder builder,
                    @Value("${sfdc.key}") String consumerKey,
                    @Value("${sfdc.secret}") String consumerSecret,
                    @Value("${sfdc.oauth.authorization-url}") String authorizationUrl,
                    @Value("${sfdc.oauth.access-token-url}") String accessTokenUrl,
                    @Value("${sfdc.oauth.display}") String display,
                    @Value("${sfdc.oauth.callback.domain}") String callbackDomain,
                    @Value("${sfdc.oauth.callback.local-port}") int callbackPort,
                    @Value("${sfdc.oauth.callback.path}") String callbackPath) {
        this.http = builder.build();
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.authorizationUrl = authorizationUrl;
        this.accessTokenUrl = accessTokenUrl;
        this.display = display;
        this.redirectUri = "http://" + callbackDomain + ":" + callbackPort + "/" + callbackPath;
    }

    boolean authorized() {
        return token != null;
    }

    /**
     * Where {@code sfdc:authorize} redirects the browser when there is no access token yet. The {@code state}
     * value is single-use and must come back on the callback, so only authorizations this app started are accepted.
     */
    String authorizationRedirect() {
        String state = UUID.randomUUID().toString();
        rememberState(state);
        return authorizationUrl
                + "?response_type=code"
                + "&client_id=" + encode(consumerKey)
                + "&redirect_uri=" + encode(redirectUri)
                + "&display=" + encode(display.toLowerCase(Locale.ROOT))
                + "&state=" + encode(state);
    }

    /** Remembers an issued state for {@link #STATE_TTL}; at most {@link #MAX_PENDING_STATES} are kept (oldest dropped). */
    synchronized void rememberState(String state) {
        Instant now = Instant.now();
        pendingStates.values().removeIf(issued -> issued.plus(STATE_TTL).isBefore(now));
        while (pendingStates.size() >= MAX_PENDING_STATES) {
            pendingStates.entrySet().stream().min(Map.Entry.comparingByValue())
                    .ifPresent(oldest -> pendingStates.remove(oldest.getKey()));
        }
        pendingStates.put(state, now);
    }

    /** True when {@code state} was issued by {@link #authorizationRedirect()}, has not expired and was not used yet. */
    boolean isPendingState(String state) {
        Instant issued = state == null ? null : pendingStates.get(state);
        return issued != null && !issued.plus(STATE_TTL).isBefore(Instant.now());
    }

    /**
     * OAuth callback: exchanges the authorization code at the access token URL and stores the token. The state is
     * used up only once the exchange succeeded, so a callback that failed on a transient error can be retried.
     */
    void exchange(String code, String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", consumerKey);
        form.add("client_secret", consumerSecret);
        form.add("redirect_uri", redirectUri);
        token = requestToken(form, null);
        pendingStates.remove(state);
    }

    @Override
    public SalesforceToken current() {
        SalesforceToken current = token;
        if (current == null) {
            throw new IllegalStateException("Salesforce is not authorized yet");
        }
        return current;
    }

    @Override
    public synchronized SalesforceToken renew(SalesforceToken stale) {
        if (token != null && !token.equals(stale)) {
            return token;
        }
        if (stale.refreshToken() == null) {
            token = null;
            throw new IllegalStateException("Salesforce access token expired and there is no refresh token");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", stale.refreshToken());
        form.add("client_id", consumerKey);
        form.add("client_secret", consumerSecret);
        token = requestToken(form, stale.refreshToken());
        return token;
    }

    /** Replaces the stored token (tests, or restoring a persisted token). */
    void store(SalesforceToken token) {
        this.token = token;
    }

    private SalesforceToken requestToken(MultiValueMap<String, String> form, String previousRefreshToken) {
        JsonNode body = http.post().uri(accessTokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        String refresh = body.hasNonNull("refresh_token") ? body.get("refresh_token").asText() : previousRefreshToken;
        return new SalesforceToken(body.path("access_token").asText(), body.path("instance_url").asText(), refresh);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
