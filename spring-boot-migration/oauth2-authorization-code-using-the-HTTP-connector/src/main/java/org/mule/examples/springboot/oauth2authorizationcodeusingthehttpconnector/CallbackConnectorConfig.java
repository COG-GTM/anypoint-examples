package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * Mule opens the localAuthorizationUrl/redirectionUrl listeners on their own host:port (8082, TLS
 * via {@code TLS_Context}), separate from {@code HTTP_Listener_Configuration} (8081). Spring adds a
 * second Tomcat connector for that port and keeps each path on the port Mule served it on.
 */
@Configuration
@ConditionalOnProperty(name = "oauth.callback.connector-enabled", havingValue = "true", matchIfMissing = true)
class CallbackConnectorConfig {

    private final URI localAuthorizationUrl;
    private final URI redirectionUrl;

    CallbackConnectorConfig(@Value("${oauth.local-authorization-url}") String localAuthorizationUrl,
                            @Value("${oauth.redirection-url}") String redirectionUrl) {
        this.localAuthorizationUrl = URI.create(localAuthorizationUrl);
        this.redirectionUrl = URI.create(redirectionUrl);
    }

    int callbackPort() {
        int port = localAuthorizationUrl.getPort();
        return port != -1 ? port : ("https".equals(localAuthorizationUrl.getScheme()) ? 443 : 80);
    }

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> callbackConnector(
            @Value("${oauth.callback.ssl.key-store:}") String keyStore,
            @Value("${oauth.callback.ssl.key-store-password:}") String keyStorePassword,
            @Value("${oauth.callback.ssl.key-password:}") String keyPassword) {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(callbackPort());
            if (!keyStore.isBlank()) {
                connector.setScheme("https");
                connector.setSecure(true);
                ((Http11NioProtocol) connector.getProtocolHandler()).setSSLEnabled(true);
                SSLHostConfig ssl = new SSLHostConfig();
                SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(ssl, SSLHostConfigCertificate.Type.UNDEFINED);
                certificate.setCertificateKeystoreFile(keyStore);
                certificate.setCertificateKeystorePassword(keyStorePassword);
                certificate.setCertificateKeyPassword(keyPassword.isBlank() ? keyStorePassword : keyPassword);
                ssl.addCertificate(certificate);
                connector.addSslHostConfig(ssl);
            }
            factory.addAdditionalTomcatConnectors(connector);
        };
    }

    @Bean
    FilterRegistrationBean<OncePerRequestFilter> listenerPortSeparation() {
        Set<String> callbackPaths = Set.of(OAuthCallbackEndpoints.path(localAuthorizationUrl.toString()),
                OAuthCallbackEndpoints.path(redirectionUrl.toString()));
        int callbackPort = callbackPort();
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                boolean onCallbackPort = request.getLocalPort() == callbackPort;
                boolean callbackPath = callbackPaths.contains(request.getRequestURI());
                if (onCallbackPort != callbackPath) {
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
