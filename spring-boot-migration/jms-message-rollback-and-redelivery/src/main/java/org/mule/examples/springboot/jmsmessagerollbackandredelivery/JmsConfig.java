package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
class JmsConfig {

    private static final Logger log = LoggerFactory.getLogger(JmsConfig.class);

    /** The {@code redeliveryPolicy} Spring bean of jms-redelivery.xml, set on the ActiveMQ connection factory. */
    @Bean
    ActiveMQConnectionFactoryCustomizer redeliveryPolicyCustomizer(
            @Value("${activemq.redelivery-policy.maximum-redeliveries}") int maximumRedeliveries,
            @Value("${activemq.redelivery-policy.initial-redelivery-delay}") long initialRedeliveryDelay,
            @Value("${activemq.redelivery-policy.redelivery-delay}") long redeliveryDelay,
            @Value("${activemq.redelivery-policy.use-exponential-back-off}") boolean useExponentialBackOff,
            @Value("${activemq.redelivery-policy.back-off-multiplier}") double backOffMultiplier) {
        return factory -> {
            RedeliveryPolicy policy = new RedeliveryPolicy();
            policy.setMaximumRedeliveries(maximumRedeliveries);
            policy.setInitialRedeliveryDelay(initialRedeliveryDelay);
            policy.setRedeliveryDelay(redeliveryDelay);
            policy.setUseExponentialBackOff(useExponentialBackOff);
            policy.setBackOffMultiplier(backOffMultiplier);
            factory.setRedeliveryPolicy(policy);
        };
    }

    /** {@code jms:transaction action="ALWAYS_BEGIN"} on the inbound endpoint: a transacted JMS session per message. */
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
