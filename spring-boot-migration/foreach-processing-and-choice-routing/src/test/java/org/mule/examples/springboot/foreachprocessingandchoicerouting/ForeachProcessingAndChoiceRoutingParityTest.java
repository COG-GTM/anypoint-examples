package org.mule.examples.springboot.foreachprocessingandchoicerouting;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

/** Credit agency and banks are WireMock stubs with fixed rates; expectedOutbound pins the banks the choice routed to. */
class ForeachProcessingAndChoiceRoutingParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/foreach-processing-and-choice-routing";
    }
}
