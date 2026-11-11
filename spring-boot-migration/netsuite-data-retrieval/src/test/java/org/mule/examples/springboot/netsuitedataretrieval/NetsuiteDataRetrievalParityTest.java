package org.mule.examples.springboot.netsuitedataretrieval;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

class NetsuiteDataRetrievalParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/netsuite-data-retrieval";
    }
}
