package org.mule.examples.springboot.webspheremq;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Subscribers of the {@code /services/wmqExample/dequeue} channel (one SSE stream per browser). */
@Component
public class DequeueChannel {

    private final List<SseEmitter> subscribers = new CopyOnWriteArrayList<>();

    SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        subscribers.add(emitter);
        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(() -> subscribers.remove(emitter));
        emitter.onError(e -> subscribers.remove(emitter));
        try {
            // flushes the response headers so the browser knows the subscription is live
            emitter.send(SseEmitter.event().comment("subscribed"));
        } catch (IOException e) {
            subscribers.remove(emitter);
        }
        return emitter;
    }

    void publish(String data) {
        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException | IllegalStateException e) {
                subscribers.remove(emitter);
            }
        }
    }
}
