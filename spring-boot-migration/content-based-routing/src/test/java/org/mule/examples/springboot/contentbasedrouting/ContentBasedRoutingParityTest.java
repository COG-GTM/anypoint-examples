package org.mule.examples.springboot.contentbasedrouting;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

class ContentBasedRoutingParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/content-based-routing";
    }
}
