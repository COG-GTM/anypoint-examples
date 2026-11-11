package org.mule.examples.springboot.sendingjsondatatoaamqpqueue;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/** The exchange, queue and binding the README (steps 2-4) and the Mule IT create by hand. */
@Configuration
@ConditionalOnProperty(name = "amqp.declare-topology", havingValue = "true", matchIfMissing = true)
class AmqpTopologyConfig {

    @Bean
    TopicExchange salesExchange(@Value("${amqp.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    Queue salesQueue(@Value("${amqp.queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    Binding salesBinding(Queue salesQueue, TopicExchange salesExchange) {
        return BindingBuilder.bind(salesQueue).to(salesExchange).with("#");
    }
}
