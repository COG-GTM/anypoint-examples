package org.mule.examples.springboot.jmsmessagerollbackandredelivery;


class RedeliveryParityTest extends AbstractRedeliveryParityTest {

    @Override
    protected String fixtureSubdirectory() {
        return "redelivery";
    }
}
