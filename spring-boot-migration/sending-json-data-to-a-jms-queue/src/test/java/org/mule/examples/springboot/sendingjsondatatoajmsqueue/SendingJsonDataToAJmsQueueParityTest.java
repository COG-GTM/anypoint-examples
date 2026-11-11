package org.mule.examples.springboot.sendingjsondatatoajmsqueue;

import org.mule.examples.springboot.parity.AbstractParityTest;
import org.mule.examples.springboot.parity.CapturedResponse;
import org.mule.examples.springboot.parity.MessagingParityAssertions;
import org.mule.examples.springboot.parity.MessagingParityFixture;
import org.mule.examples.springboot.parity.MessagingParityFixtures;
import org.mule.examples.springboot.parity.ParityContainers;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

/**
 * Runs the json-to-jmsFlow fixtures against the Spring Boot endpoint with a real ActiveMQ broker
 * (Testcontainers) and asserts what lands on queue "sales".
 */
class SendingJsonDataToAJmsQueueParityTest extends AbstractParityTest {

    static final String DIR = "parity/sending-json-data-to-a-jms-queue";
    static final GenericContainer<?> ACTIVEMQ = ParityContainers.activeMq();

    static {
        ACTIVEMQ.start();
    }

    @DynamicPropertySource
    static void broker(DynamicPropertyRegistry registry) {
        registry.add("spring.activemq.broker-url",
                () -> "tcp://" + ACTIVEMQ.getHost() + ":" + ACTIVEMQ.getMappedPort(61616));
    }

    @Autowired
    JmsTemplate jms;

    @Override
    protected String fixtureDirectory() {
        return DIR;
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        JmsCapture.drain(jms, "sales", 200);
    }

    @Override
    protected void afterCase(ParityFixture fixture, CapturedResponse spring) {
        MessagingParityFixture messaging = MessagingParityFixtures.byName(DIR, fixture.name());
        for (MessagingParityFixture.MessageExpectation expected : messaging.expectedMessages()) {
            String queue = expected.destination().replaceFirst("^queue:", "");
            MessagingParityAssertions.assertMessages(messaging, expected,
                    JmsCapture.drain(jms, queue, expected.count() > 0 ? 5000 : 1000));
        }
    }
}
