package org.mule.examples.springboot.mulecomponentbindings;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

/** Stocklytics, Twitter and Sentiment140 are WireMock stubs; expectedOutbound pins the paging calls of the binding. */
class MuleComponentBindingsParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/mule-component-bindings";
    }
}
