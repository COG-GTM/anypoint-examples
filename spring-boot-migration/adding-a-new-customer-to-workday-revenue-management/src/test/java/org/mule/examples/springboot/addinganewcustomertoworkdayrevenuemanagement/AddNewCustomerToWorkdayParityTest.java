package org.mule.examples.springboot.addinganewcustomertoworkdayrevenuemanagement;

import org.mule.examples.springboot.parity.AbstractWireMockParityTest;

class AddNewCustomerToWorkdayParityTest extends AbstractWireMockParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/adding-a-new-customer-to-workday-revenue-management";
    }
}
