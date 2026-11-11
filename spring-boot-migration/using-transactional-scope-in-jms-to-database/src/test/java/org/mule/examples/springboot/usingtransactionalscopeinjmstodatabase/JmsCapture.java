package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;
import org.mule.examples.springboot.parity.MessagingParityAssertions.CapturedMessage;
import org.springframework.jms.core.JmsTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads every message currently on a queue and converts it to a {@link CapturedMessage}. */
final class JmsCapture {

    private JmsCapture() {
    }

    static List<CapturedMessage> drain(JmsTemplate jms, String queue, long firstWaitMillis) {
        List<CapturedMessage> captured = new ArrayList<>();
        long wait = firstWaitMillis;
        while (true) {
            jms.setReceiveTimeout(wait);
            Message m = jms.receive(queue);
            if (m == null) {
                return captured;
            }
            captured.add(toCaptured(m));
            wait = 500;
        }
    }

    static CapturedMessage toCaptured(Message m) {
        try {
            Map<String, String> props = new LinkedHashMap<>();
            for (Object name : Collections.list(m.getPropertyNames())) {
                props.put(name.toString(), String.valueOf(m.getObjectProperty(name.toString())));
            }
            if (m.getJMSCorrelationID() != null) {
                props.put("JMSCorrelationID", m.getJMSCorrelationID());
            }
            if (m instanceof TextMessage t) {
                return new CapturedMessage("TextMessage", t.getText(), props);
            }
            if (m instanceof BytesMessage b) {
                byte[] bytes = new byte[(int) b.getBodyLength()];
                b.readBytes(bytes);
                return new CapturedMessage("BytesMessage", new String(bytes, StandardCharsets.UTF_8), props);
            }
            if (m instanceof ObjectMessage o) {
                return new CapturedMessage("ObjectMessage", String.valueOf(o.getObject()), props);
            }
            return new CapturedMessage(m.getClass().getSimpleName(), null, props);
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }
    }
}
