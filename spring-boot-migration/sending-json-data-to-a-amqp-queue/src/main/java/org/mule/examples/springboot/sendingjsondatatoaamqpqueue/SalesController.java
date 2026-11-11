package org.mule.examples.springboot.sendingjsondatatoaamqpqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Flow {@code json-to-rabbitmqFlow}: HTTP listener (POST /) → logger → AMQP outbound endpoint
 * (exchange sales_exchange, queue sales_queue, exchange-pattern request-response, responseTimeout 10000).
 * The flow's result (the AMQP reply, or nothing when no reply arrives in time) is the HTTP response body.
 */
@RestController
public class SalesController {

    private static final Logger log = LoggerFactory.getLogger(SalesController.class);

    private final RabbitTemplate rabbit;
    private final String exchange;
    private final String routingKey;

    public SalesController(RabbitTemplate rabbit,
                           @Value("${amqp.exchange}") String exchange,
                           @Value("${amqp.routing-key}") String routingKey,
                           @Value("${amqp.response-timeout}") long responseTimeout) {
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.routingKey = routingKey;
        rabbit.setReplyTimeout(responseTimeout);
    }

    @PostMapping("/")
    public ResponseEntity<byte[]> send(@RequestBody(required = false) byte[] body) {
        byte[] payload = body == null ? new byte[0] : body;
        log.info("payload to be sent: {}", new String(payload, StandardCharsets.UTF_8));
        Message reply = rabbit.sendAndReceive(exchange, routingKey, new Message(payload, new MessageProperties()));
        if (reply == null) {
            log.info("No AMQP reply received from exchange {}", exchange);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(reply.getBody());
    }
}
