package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"jms.max-redelivery=-1", "activemq.redelivery-policy.maximum-redeliveries=3"})
class DeadLetterParityTest extends AbstractRedeliveryParityTest {

    @Override
    protected String fixtureSubdirectory() {
        return "dead-letter";
    }
}
