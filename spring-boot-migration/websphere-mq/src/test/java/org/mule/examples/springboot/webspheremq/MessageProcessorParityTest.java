package org.mule.examples.springboot.webspheremq;

import org.springframework.test.context.TestPropertySource;

/** Output flow switched off so that what MessageProcessor puts on WMQ "out" can be read directly. */
@TestPropertySource(properties = "wmq.output.enabled=false")
class MessageProcessorParityTest extends AbstractWebsphereMqParityTest {

    @Override
    protected String fixtureSubdirectory() {
        return "message-processor";
    }
}
