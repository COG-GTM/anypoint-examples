package org.mule.examples.springboot.oauth2clientcredentialsusingthehttpconnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Mule {@code oauth2:client-credentials-grant-type} inside {@code HTTP_Request_Configuration} →
 * Spring Security OAuth2 Client: a {@link ClientRegistration} for Box plus an authorized-client
 * manager that runs the client-credentials grant against the token URL.
 */
@Configuration
class OAuthClientConfig {

    static final String REGISTRATION_ID = "box";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(@Value("${oauth.client.id}") String clientId,
                                                              @Value("${oauth.client.secret}") String clientSecret,
                                                              @Value("${box.token-url}") String tokenUrl) {
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tokenUri(tokenUrl)
                .build());
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                          OAuth2AuthorizedClientService clients) {
        // Mule's ClientCredentialsTokenRequestHandler sends "Authorization: Basic base64(id:secret)" with the
        // raw id/secret; Spring URL-encodes them first, so the header is built the Mule way here.
        OAuth2ClientCredentialsGrantRequestEntityConverter converter = new OAuth2ClientCredentialsGrantRequestEntityConverter();
        converter.setHeadersConverter(OAuthClientConfig::muleTokenRequestHeaders);
        DefaultClientCredentialsTokenResponseClient tokenClient = new DefaultClientCredentialsTokenResponseClient();
        tokenClient.setRequestEntityConverter(converter);

        ClientCredentialsOAuth2AuthorizedClientProvider provider = new ClientCredentialsOAuth2AuthorizedClientProvider();
        provider.setAccessTokenResponseClient(tokenClient);
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    static HttpHeaders muleTokenRequestHeaders(OAuth2ClientCredentialsGrantRequest request) {
        ClientRegistration registration = request.getClientRegistration();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String credentials = registration.getClientId() + ":" + registration.getClientSecret();
        headers.set(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        return headers;
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
