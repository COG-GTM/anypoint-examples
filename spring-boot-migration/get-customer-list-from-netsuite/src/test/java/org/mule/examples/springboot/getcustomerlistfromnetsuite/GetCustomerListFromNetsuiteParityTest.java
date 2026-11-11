package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

class GetCustomerListFromNetsuiteParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/get-customer-list-from-netsuite";
    }
}
