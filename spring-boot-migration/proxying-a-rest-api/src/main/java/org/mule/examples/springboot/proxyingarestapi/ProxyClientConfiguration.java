package org.mule.examples.springboot.proxyingarestapi;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

/** Mule {@code HTTP_Request_Configuration}: the target API base URL plus the optional TLS key store. */
@Configuration
class ProxyClientConfiguration {

    @Bean
    WebClient proxyWebClient(@Value("${proxy.target.base-url}") String baseUrl,
                             @Value("${keystore.path:}") String keystorePath,
                             @Value("${keystore.key:}") String storePassword,
                             @Value("${keystore.password:}") String keyPassword) throws Exception {
        // Mule's http:request follows redirects by default (followRedirects="true").
        HttpClient http = HttpClient.create().followRedirect(true);
        if (!keystorePath.isBlank()) {
            SslContext ssl = SslContextBuilder.forClient()
                    .keyManager(keyManagers(keystorePath, storePassword, keyPassword))
                    .build();
            http = http.secure(spec -> spec.sslContext(ssl));
        }
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(-1))
                        .build())
                .build();
    }

    private static KeyManagerFactory keyManagers(String path, String storePassword, String keyPassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new DefaultResourceLoader().getResource(path.contains(":") ? path : "classpath:" + path).getInputStream()) {
            keyStore.load(in, storePassword.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPassword.toCharArray());
        return kmf;
    }
}
