package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The OAuth dance Mule's {@code oauth2:authorization-code-grant-type} runs: the local authorization
 * redirect ({@code AuthorizationRequestHandler}), the redirect-URL callback that exchanges the code
 * ({@code AutoAuthorizationCodeTokenRequestHandler}), and the token used/refreshed by requests
 * ({@code DefaultAuthorizationCodeGrantType}). Spring Security's token response clients perform the
 * token calls; responses and status codes follow the Mule classes named above.
 */
@Component
public class BoxAuthorizationCodeClient {

    static final String DEFAULT_RESOURCE_OWNER = "default";
    static final int SUCCESS_STATUS = 0;
    static final int NO_AUTHORIZATION_CODE_STATUS = 100;
    static final int TOKEN_URL_CALL_FAILED_STATUS = 200;
    static final int TOKEN_NOT_FOUND_STATUS = 201;
    static final int FAILURE_PROCESSING_REDIRECT_URL_REQUEST_STATUS = 300;

    /** oauth2:custom-parameters, in the iteration order of the HashMap Mule keeps them in. */
    static final Map<String, String> CUSTOM_PARAMETERS = new LinkedHashMap<>();

    static {
        CUSTOM_PARAMETERS.put("box_device_id", "1");
        CUSTOM_PARAMETERS.put("box_device_name", "device1");
    }

    private static final Logger log = LoggerFactory.getLogger(BoxAuthorizationCodeClient.class);

    private final ClientRegistration registration;
    private final OAuth2AuthorizedClientService clients;
    private final DefaultAuthorizationCodeTokenResponseClient codeClient;
    private final DefaultRefreshTokenTokenResponseClient refreshClient;

    public BoxAuthorizationCodeClient(ClientRegistrationRepository registrations, OAuth2AuthorizedClientService clients,
                                      DefaultAuthorizationCodeTokenResponseClient codeClient,
                                      DefaultRefreshTokenTokenResponseClient refreshClient) {
        this.registration = registrations.findByRegistrationId(OAuthClientConfig.REGISTRATION_ID);
        this.clients = clients;
        this.codeClient = codeClient;
        this.refreshClient = refreshClient;
    }

    public record CallbackResult(int httpStatus, String location, String body, int authorizationStatus) {
    }

    /** Location of the 302 sent by the localAuthorizationUrl listener (AuthorizationRequestUrlBuilder order). */
    public String authorizationRequestLocation(String onCompleteRedirectTo) {
        String state = new MuleOAuthState(null, null, onCompleteRedirectTo).encode();
        return authorizationRequest(state).getAuthorizationRequestUri();
    }

    private OAuth2AuthorizationRequest authorizationRequest(String state) {
        StringBuilder url = new StringBuilder(registration.getProviderDetails().getAuthorizationUri())
                .append("?response_type=code&client_id=").append(registration.getClientId());
        if (state != null && !state.isBlank()) {
            url.append("&state=").append(encode(state));
        }
        CUSTOM_PARAMETERS.forEach((k, v) -> url.append('&').append(k).append('=').append(encode(v)));
        url.append("&redirect_uri=").append(encode(registration.getRedirectUri()));
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(registration.getRedirectUri())
                .state(state)
                .additionalParameters(Map.copyOf(CUSTOM_PARAMETERS))
                .authorizationRequestUri(url.toString())
                .build();
    }

    /** The redirectionUrl listener: exchange the code, store the tokens, answer like Mule. */
    public CallbackResult handleRedirect(String code, String state, String requestUri) {
        MuleOAuthState decoded = MuleOAuthState.decode(state);
        int authorizationStatus;
        int httpStatus;
        String message;
        if (code == null) {
            log.error("Could not extract authorization code from OAuth provider HTTP request done to the redirect URL");
            authorizationStatus = NO_AUTHORIZATION_CODE_STATUS;
            httpStatus = 400;
            message = "Failure retrieving access token.\n OAuth Server uri from callback: " + requestUri;
        } else {
            try {
                OAuth2AccessTokenResponse tokens = codeClient.getTokenResponse(new OAuth2AuthorizationCodeGrantRequest(registration,
                        new OAuth2AuthorizationExchange(authorizationRequest(state),
                                OAuth2AuthorizationResponse.success(code).redirectUri(registration.getRedirectUri()).state(state).build())));
                if (tokens.getRefreshToken() == null) {
                    log.error("Could not extract access token or refresh token from token URL. Refresh token is null");
                    authorizationStatus = TOKEN_NOT_FOUND_STATUS;
                    httpStatus = 500;
                    message = "Failed getting access token or refresh token from token URL response. See logs for details.";
                } else {
                    String owner = decoded.resourceOwnerId() == null ? DEFAULT_RESOURCE_OWNER : decoded.resourceOwnerId();
                    store(owner, tokens);
                    authorizationStatus = SUCCESS_STATUS;
                    httpStatus = 200;
                    message = "Successfully retrieved access token";
                }
            } catch (OAuth2AuthorizationException e) {
                if (TokenUrlStatusException.isCause(e)) {
                    log.error("HTTP response from token URL {} returned a failure status code", tokenUrl());
                    authorizationStatus = TOKEN_URL_CALL_FAILED_STATUS;
                    httpStatus = 500;
                    // Mule's TokenUrlResponseException carries no message
                    message = String.format("Failure calling token url %s. Exception message is %s", tokenUrl(), (Object) null);
                } else {
                    log.error("Fail processing redirect URL request", e);
                    authorizationStatus = FAILURE_PROCESSING_REDIRECT_URL_REQUEST_STATUS;
                    httpStatus = 500;
                    message = "Failed processing redirect URL request done from OAuth provider. See logs for details.";
                }
            }
        }
        String location = null;
        if (decoded.onCompleteRedirectTo() != null && !decoded.onCompleteRedirectTo().isEmpty()) {
            httpStatus = 302;
            String target = decoded.onCompleteRedirectTo();
            location = target + (target.contains("?") ? "&" : "?") + "authorizationStatus=" + authorizationStatus;
        }
        return new CallbackResult(httpStatus, location, message, authorizationStatus);
    }

    /** Access token for the default resource owner, as DefaultAuthorizationCodeGrantType#authenticate. */
    public String accessToken() {
        OAuth2AuthorizedClient client = clients.loadAuthorizedClient(OAuthClientConfig.REGISTRATION_ID, DEFAULT_RESOURCE_OWNER);
        if (client == null) {
            throw new MuleFlowException(String.format("No access token for the %s user. Verify that you have authenticated "
                    + "the user before trying to execute an operation to the API.", DEFAULT_RESOURCE_OWNER));
        }
        return client.getAccessToken().getTokenValue();
    }

    /** refreshTokenWhen matched (401/403): refresh_token grant against the token URL. */
    public synchronized void refreshAccessToken() {
        OAuth2AuthorizedClient client = clients.loadAuthorizedClient(OAuthClientConfig.REGISTRATION_ID, DEFAULT_RESOURCE_OWNER);
        if (client == null || client.getRefreshToken() == null) {
            throw new MuleFlowException("No refresh token for the " + DEFAULT_RESOURCE_OWNER + " user.");
        }
        OAuth2AccessTokenResponse tokens;
        try {
            tokens = refreshClient.getTokenResponse(new OAuth2RefreshTokenGrantRequest(registration,
                    client.getAccessToken(), client.getRefreshToken()));
        } catch (OAuth2AuthorizationException e) {
            throw new MuleFlowException("Failure calling token url " + tokenUrl() + " to refresh the access token", e);
        }
        if (tokens.getRefreshToken() == null) {
            throw new MuleFlowException("Failed getting access token or refresh token from token URL response. See logs for details.");
        }
        store(DEFAULT_RESOURCE_OWNER, tokens);
    }

    public void forgetTokens() {
        clients.removeAuthorizedClient(OAuthClientConfig.REGISTRATION_ID, DEFAULT_RESOURCE_OWNER);
    }

    private void store(String owner, OAuth2AccessTokenResponse tokens) {
        clients.saveAuthorizedClient(new OAuth2AuthorizedClient(registration, owner, tokens.getAccessToken(), tokens.getRefreshToken()),
                new PreAuthenticatedAuthenticationToken(owner, "n/a"));
    }

    private String tokenUrl() {
        return registration.getProviderDetails().getTokenUri();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
