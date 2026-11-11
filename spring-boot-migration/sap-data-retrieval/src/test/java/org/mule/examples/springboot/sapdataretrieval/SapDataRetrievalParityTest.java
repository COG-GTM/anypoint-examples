package org.mule.examples.springboot.sapdataretrieval;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

class SapDataRetrievalParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/sap-data-retrieval";
    }
}
