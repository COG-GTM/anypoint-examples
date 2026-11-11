package org.mule.examples.springboot.proxyingasoapapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Mule {@code HTTP_Request_Configuration} (host www.predic8.com, port 8080). */
@Configuration
class ShopServiceClientConfiguration {

    @Bean
    WebClient shopServiceClient(@Value("${soap.target.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(-1))
                .build();
    }
}
