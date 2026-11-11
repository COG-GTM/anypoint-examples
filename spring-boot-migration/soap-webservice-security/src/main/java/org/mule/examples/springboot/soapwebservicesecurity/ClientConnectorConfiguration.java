package org.mule.examples.springboot.soapwebservicesecurity;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mule's second listener ({@code HTTP_Listener_Configuration}, port 63080) for the {@code SecurityClients} flow. */
@Configuration
class ClientConnectorConfiguration {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> clientConnector(@Value("${security.client.port:63080}") int port) {
        return factory -> {
            if (port >= 0 && port != factory.getPort()) {
                Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                connector.setPort(port);
                factory.addAdditionalTomcatConnectors(connector);
            }
        };
    }
}
