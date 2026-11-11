package org.mule.examples.springboot.webspheremq;

import jakarta.jms.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
class JmsConfig {

    private static final Logger log = LoggerFactory.getLogger(JmsConfig.class);

    /** {@code wmq:transaction action="ALWAYS_BEGIN"}: consume from "in" and send to "out" in one MQ syncpoint. */
    @Bean
    DefaultJmsListenerContainerFactory transactedListenerFactory(DefaultJmsListenerContainerFactoryConfigurer configurer,
                                                                ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setSessionTransacted(true);
        factory.setErrorHandler(t -> log.warn("MessageProcessor rolled back: {}", t.toString()));
        return factory;
    }
}
