package org.mule.examples.springboot.webspheremq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The browser side of the AJAX connector. Flow Input: {@code POST /services/wmqExample/enqueue} (channel
 * {@code /services/wmqExample/enqueue}) puts the text on WMQ queue "in". {@code GET /services/wmqExample/dequeue}
 * is a Server-Sent Events stream standing in for the {@code /services/wmqExample/dequeue} channel subscription.
 */
@RestController
public class InputController {

    private static final Logger log = LoggerFactory.getLogger(InputController.class);

    private final JmsTemplate jms;
    private final DequeueChannel dequeue;
    private final String inQueue;

    InputController(JmsTemplate jms, DequeueChannel dequeue, @Value("${wmq.in-queue}") String inQueue) {
        this.jms = jms;
        this.dequeue = dequeue;
        this.inQueue = inQueue;
    }

    @PostMapping(path = "/services/wmqExample/enqueue", produces = MediaType.TEXT_PLAIN_VALUE)
    public String enqueue(@RequestBody(required = false) String payload) {
        String text = payload == null ? "" : payload;
        jms.convertAndSend(inQueue, text);
        log.info("Enqueued to WMQ queue {}: {}", inQueue, text);
        return text;
    }

    @GetMapping(path = "/services/wmqExample/dequeue", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter dequeue() {
        return dequeue.subscribe();
    }
}
