package org.mule.examples.springboot.filteringamessage;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

class FilteringAMessageParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/filtering-a-message";
    }
}
