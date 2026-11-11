package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

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

    /**
     * Transacted listener session: the received message and any send from the same thread (exposed listener session)
     * commit or roll back together; a thrown exception makes ActiveMQ redeliver the message.
     */
    @Bean
    DefaultJmsListenerContainerFactory transactedListenerFactory(DefaultJmsListenerContainerFactoryConfigurer configurer,
                                                                ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setSessionTransacted(true);
        factory.setConcurrency("1");
        factory.setErrorHandler(t -> log.debug("Listener rolled back", t));
        return factory;
    }
}
