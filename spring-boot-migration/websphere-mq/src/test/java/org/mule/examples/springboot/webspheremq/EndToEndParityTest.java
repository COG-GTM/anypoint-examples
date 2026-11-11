package org.mule.examples.springboot.webspheremq;

class EndToEndParityTest extends AbstractWebsphereMqParityTest {

    @Override
    protected String fixtureSubdirectory() {
        return "end-to-end";
    }
}
