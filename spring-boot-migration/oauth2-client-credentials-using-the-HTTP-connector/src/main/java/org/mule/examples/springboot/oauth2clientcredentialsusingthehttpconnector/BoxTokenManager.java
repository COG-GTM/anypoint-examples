package org.mule.examples.springboot.oauth2clientcredentialsusingthehttpconnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;

/**
 * Mule {@code oauth2:token-manager-config} for the client-credentials grant: one token for the
 * "default" resource owner, fetched when the app starts ({@code ClientCredentialsGrantType#start})
 * and replaced only when the API answers 401/403 (Mule's default {@code refreshTokenWhen}). There
 * is no expiry-based refresh, as in Mule.
 */
@Component
public class BoxTokenManager {

    static final String RESOURCE_OWNER = "default";
    static final String NO_TOKEN_MESSAGE =
            "No access token found. Verify that you have authenticated before trying to execute an operation to the API.";

    private static final Logger log = LoggerFactory.getLogger(BoxTokenManager.class);

    private final OAuth2AuthorizedClientManager manager;
    private final OAuth2AuthorizedClientService clients;

    public BoxTokenManager(OAuth2AuthorizedClientManager manager, OAuth2AuthorizedClientService clients) {
        this.manager = manager;
        this.clients = clients;
    }

    /** A failure here stops the application, like a failing token request in Mule's start phase. */
    @EventListener(ApplicationStartedEvent.class)
    public void fetchTokenOnStartup() {
        refreshAccessToken();
    }

    public synchronized void refreshAccessToken() {
        clients.removeAuthorizedClient(OAuthClientConfig.REGISTRATION_ID, RESOURCE_OWNER);
        OAuth2AuthorizedClient client = manager.authorize(OAuth2AuthorizeRequest
                .withClientRegistrationId(OAuthClientConfig.REGISTRATION_ID)
                .principal(RESOURCE_OWNER)
                .build());
        log.debug("Retrieved access token from token url, expires at {}",
                client == null ? null : client.getAccessToken().getExpiresAt());
    }

    public String accessToken() {
        OAuth2AuthorizedClient client = clients.loadAuthorizedClient(OAuthClientConfig.REGISTRATION_ID, RESOURCE_OWNER);
        if (client == null) {
            throw new MuleFlowException(NO_TOKEN_MESSAGE);
        }
        return client.getAccessToken().getTokenValue();
    }
}
