package org.mule.examples.springboot.xmlonlysoapwebservice;

import org.mule.examples.springboot.parity.AbstractParityTest;

/**
 * Replays {@code parity/xml-only-soap-webservice} fixtures. No WireMock: as in Mule, the Admission service calls the
 * Patient and EHR services on the app's own listener.
 */
class XmlOnlySoapWebserviceParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/xml-only-soap-webservice";
    }
}
