package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"jms.max-redelivery=0"})
class MaxRedeliveryExceededParityTest extends AbstractRedeliveryParityTest {

    @Override
    protected String fixtureSubdirectory() {
        return "max-redelivery-exceeded";
    }
}
