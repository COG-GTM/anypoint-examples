package org.mule.examples.springboot.httpoauthprovider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Mule {@code oauth2-provider:config} (authorization server: client, scopes, resource owner security
 * provider, token TTL) → Spring Authorization Server; {@code oauth2-provider:validate} → a Spring
 * Security resource server whose opaque-token introspector reads the authorization server's own
 * token store, as Mule's provider validates against its in-memory token store.
 */
@Configuration
class AuthorizationServerConfig {

    static final String AUTHORIZATION_PATH = "/authorize";
    static final String TOKEN_PATH = "/token";

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .authorizationEndpoint(AUTHORIZATION_PATH)
                .tokenEndpoint(TOKEN_PATH)
                .build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(@Value("${provider.client.id}") String clientId,
                                                          @Value("${provider.client.secret}") String secret,
                                                          @Value("${provider.client.name}") String name,
                                                          @Value("${provider.client.scopes}") String scopes,
                                                          @Value("${provider.token-ttl-seconds}") long ttlSeconds) {
        return new InMemoryRegisteredClientRepository(RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret("{noop}" + secret)
                .clientName(name)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // matched against provider.client.redirect-uri-patterns by MuleRedirectUriValidator
                .redirectUri("http://localhost")
                .scopes(s -> s.addAll(Arrays.stream(scopes.split(",")).map(String::trim).toList()))
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).requireProofKey(false).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofSeconds(ttlSeconds))
                        .build())
                .build());
    }

    @Bean
    OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    /** ss:authentication-manager with the single resource owner (authority RESOURCE_OWNER). */
    @Bean
    UserDetailsService resourceOwners(@Value("${provider.resource-owner.username}") String username,
                                      @Value("${provider.resource-owner.password}") String password) {
        return new InMemoryUserDetailsManager(User.withUsername(username).password("{noop}" + password)
                .authorities("RESOURCE_OWNER").build());
    }

    @Bean
    FilterRegistrationBean<LegacyTokenRequestFilter> legacyTokenRequestFilter() {
        FilterRegistrationBean<LegacyTokenRequestFilter> registration = new FilterRegistrationBean<>(new LegacyTokenRequestFilter(TOKEN_PATH));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerChain(HttpSecurity http, UserDetailsService resourceOwners,
                                                 RegisteredClientRepository clients,
                                                 @Value("${provider.name}") String providerName,
                                                 @Value("${provider.client.redirect-uri-patterns}") String redirectPatterns)
            throws Exception {
        DaoAuthenticationProvider dao = new DaoAuthenticationProvider();
        dao.setUserDetailsService(resourceOwners);
        AuthenticationManager resourceOwnerManager = new ProviderManager(dao);
        http.addFilterBefore(new LoginOnAuthorizeFilter(AUTHORIZATION_PATH, resourceOwnerManager, providerName),
                AbstractPreAuthenticatedProcessingFilter.class);

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> validator =
                OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR
                        .andThen(muleRedirectUriValidator(redirectPatterns));
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .authorizationEndpoint(a -> a.authenticationProviders(providers -> providers.forEach(p -> {
                    if (p instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider codeRequests) {
                        codeRequests.setAuthenticationValidator(validator);
                    }
                })))
                .tokenEndpoint(t -> t.accessTokenResponseHandler(muleTokenResponse()));
        http.exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) ->
                        LoginPage.render(request, response, providerName, 200, null)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.NEVER));
        return http.build();
    }

    /** Mule redirect-uri entries are patterns where '*' matches anything (e.g. http://localhost*). */
    static Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> muleRedirectUriValidator(String patterns) {
        List<Pattern> allowed = Arrays.stream(patterns.split(",")).map(String::trim)
                .map(p -> Pattern.compile(Arrays.stream(p.split("\\*", -1)).map(Pattern::quote)
                        .reduce((a, b) -> a + ".*" + b).orElse("")))
                .toList();
        return context -> {
            OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
            String redirectUri = request.getRedirectUri();
            if (redirectUri == null || allowed.stream().noneMatch(p -> p.matcher(redirectUri).matches())) {
                throw new OAuth2AuthorizationCodeRequestAuthenticationException(
                        new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "OAuth 2.0 Parameter: redirect_uri", null), null);
            }
        };
    }

    /** Token response as Mule's provider writes it: scope, expires_in (the TTL), token_type "bearer", access_token. */
    static AuthenticationSuccessHandler muleTokenResponse() {
        ObjectMapper json = new ObjectMapper();
        return (request, response, authentication) -> {
            OAuth2AccessToken token = ((OAuth2AccessTokenAuthenticationToken) authentication).getAccessToken();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("scope", String.join(" ", token.getScopes()));
            body.put("expires_in", ChronoUnit.SECONDS.between(token.getIssuedAt(), token.getExpiresAt()));
            body.put("token_type", "bearer");
            body.put("access_token", token.getTokenValue());
            response.setStatus(200);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            json.writeValue(response.getOutputStream(), body);
        };
    }

    /** Resource listener: /resources requires a READ_RESOURCE token, /redirect is open. */
    @Bean
    @Order(2)
    SecurityFilterChain resourceServerChain(HttpSecurity http, OpaqueTokenIntrospector introspector) throws Exception {
        return http.authorizeHttpRequests(a -> a
                        .requestMatchers("/resources", "/resources/**").hasAuthority("SCOPE_READ_RESOURCE")
                        .anyRequest().permitAll())
                .oauth2ResourceServer(o -> o.opaqueToken(t -> t.introspector(introspector)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .build();
    }

    /** Validates bearer tokens against the authorization server's token store (no remote introspection). */
    @Bean
    OpaqueTokenIntrospector localIntrospector(OAuth2AuthorizationService authorizations) {
        return token -> {
            OAuth2Authorization authorization = authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
            OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                    authorization == null ? null : authorization.getAccessToken();
            if (accessToken == null || !accessToken.isActive()) {
                throw new BadOpaqueTokenException("Invalid access token");
            }
            Set<String> scopes = accessToken.getToken().getScopes();
            List<GrantedAuthority> authorities = scopes.stream()
                    .<GrantedAuthority>map(s -> new SimpleGrantedAuthority("SCOPE_" + s)).toList();
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("sub", authorization.getPrincipalName());
            attributes.put("client_id", authorization.getRegisteredClientId());
            attributes.put("scope", String.join(" ", scopes));
            attributes.put("exp", accessToken.getToken().getExpiresAt() == null ? Instant.MAX : accessToken.getToken().getExpiresAt());
            return new OAuth2IntrospectionAuthenticatedPrincipal(authorization.getPrincipalName(), attributes, authorities);
        };
    }
}
