package org.mule.examples.springboot.httpoauthprovider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Mule serves the provider (/authorize, /token) on {@code http.provider.port} and the resource
 * listener (/resources, /redirect) on {@code http.listener.port}. Spring adds a second Tomcat
 * connector for the provider port and keeps each path on its Mule port.
 */
@Configuration
@ConditionalOnProperty(name = "provider.connector-enabled", havingValue = "true", matchIfMissing = true)
class ProviderConnectorConfig {

    static final Set<String> PROVIDER_PATHS = Set.of(AuthorizationServerConfig.AUTHORIZATION_PATH, AuthorizationServerConfig.TOKEN_PATH);
    static final Set<String> LISTENER_PATHS = Set.of("/resources", "/redirect");

    private final int providerPort;

    ProviderConnectorConfig(@Value("${http.provider.port}") int providerPort) {
        this.providerPort = providerPort;
    }

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> providerConnector() {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(providerPort);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }

    @Bean
    FilterRegistrationBean<OncePerRequestFilter> listenerPortSeparation() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                boolean onProviderPort = request.getLocalPort() == providerPort;
                String path = request.getRequestURI();
                if ((onProviderPort && LISTENER_PATHS.contains(path)) || (!onProviderPort && PROVIDER_PATHS.contains(path))) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                chain.doFilter(request, response);
            }
        });
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
