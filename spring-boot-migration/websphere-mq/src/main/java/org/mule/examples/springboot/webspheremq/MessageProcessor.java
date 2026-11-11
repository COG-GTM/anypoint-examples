package org.mule.examples.springboot.webspheremq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Flow MessageProcessor: transacted read from "in", {@code test:component appendString=" - processed"
 * waitTime="15000"}, then send to "out" in the same transaction ({@code JOIN_IF_POSSIBLE}).
 */
@Component
public class MessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessor.class);

    private final JmsTemplate jms;
    private final String outQueue;
    private final String appendString;
    private final long waitMillis;

    MessageProcessor(JmsTemplate jms, @Value("${wmq.out-queue}") String outQueue,
                     @Value("${wmq.processor.append-string}") String appendString,
                     @Value("${wmq.processor.wait-millis}") long waitMillis) {
        this.jms = jms;
        this.outQueue = outQueue;
        this.appendString = appendString;
        this.waitMillis = waitMillis;
    }

    @JmsListener(destination = "${wmq.in-queue}", containerFactory = "transactedListenerFactory")
    public void process(String payload) throws InterruptedException {
        log.info("MessageProcessor received: {}", payload);
        Thread.sleep(waitMillis);
        String processed = payload + appendString;
        jms.convertAndSend(outQueue, processed);
    }
}
