package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mule {@code oauth2:authorization-code-grant-type} + {@code oauth2:token-manager-config} →
 * Spring Security OAuth2 Client: a {@link ClientRegistration} for Box (client secret sent in the
 * token request body, as Mule does), the authorization-code and refresh-token token response
 * clients, and an in-memory authorized-client store keyed by resource owner.
 */
@Configuration
class OAuthClientConfig {

    static final String REGISTRATION_ID = "box";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(@Value("${box.id}") String clientId,
                                                              @Value("${box.secret}") String clientSecret,
                                                              @Value("${box.authorization-url}") String authorizationUrl,
                                                              @Value("${box.token-url}") String tokenUrl,
                                                              @Value("${oauth.redirection-url}") String redirectionUrl) {
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientId(clientId.isEmpty() ? "unset" : clientId)
                .clientSecret(clientSecret)
                .authorizationUri(authorizationUrl)
                .tokenUri(tokenUrl)
                .redirectUri(redirectionUrl)
                .build());
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    DefaultAuthorizationCodeTokenResponseClient authorizationCodeTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
        client.setRestOperations(tokenRestTemplate());
        return client;
    }

    /** Mule's refresh request also carries redirect_uri (AbstractAuthorizationCodeTokenRequestHandler). */
    @Bean
    DefaultRefreshTokenTokenResponseClient refreshTokenTokenResponseClient() {
        OAuth2RefreshTokenGrantRequestEntityConverter converter = new OAuth2RefreshTokenGrantRequestEntityConverter();
        converter.addParametersConverter(request -> {
            MultiValueMap<String, String> extra = new LinkedMultiValueMap<>();
            extra.add(OAuth2ParameterNames.REDIRECT_URI, request.getClientRegistration().getRedirectUri());
            return extra;
        });
        DefaultRefreshTokenTokenResponseClient client = new DefaultRefreshTokenTokenResponseClient();
        client.setRequestEntityConverter(converter);
        client.setRestOperations(tokenRestTemplate());
        return client;
    }

    /**
     * Mule extracts tokens with expressions and never requires token_type; Spring needs one, so a
     * missing token_type is read as "bearer" (the only type Mule's requester sends).
     */
    static RestTemplate tokenRestTemplate() {
        DefaultMapOAuth2AccessTokenResponseConverter defaults = new DefaultMapOAuth2AccessTokenResponseConverter();
        OAuth2AccessTokenResponseHttpMessageConverter tokenConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
        tokenConverter.setAccessTokenResponseConverter(parameters -> {
            Map<String, Object> withType = new HashMap<>(parameters);
            withType.putIfAbsent(OAuth2ParameterNames.TOKEN_TYPE, "bearer");
            return defaults.convert(withType);
        });
        RestTemplate rest = new RestTemplate(List.of(new FormHttpMessageConverter(), tokenConverter));
        rest.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
                return response.getStatusCode().value() >= 400;
            }

            @Override
            public void handleError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
                throw new TokenUrlStatusException(response.getStatusCode().value());
            }
        });
        return rest;
    }

    /** The Mule listeners are unauthenticated; Spring Security is only used as an OAuth2 client here. */
    @Bean
    SecurityFilterChain permitAll(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .build();
    }
}
