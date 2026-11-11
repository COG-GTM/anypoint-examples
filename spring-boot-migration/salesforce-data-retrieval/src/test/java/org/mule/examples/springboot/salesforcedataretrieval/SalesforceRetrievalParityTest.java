package org.mule.examples.springboot.salesforcedataretrieval;

import org.mule.examples.springboot.salesforcedataretrieval.salesforce.SalesforcePasswordSession;
import org.mule.examples.springboot.parity.AbstractWireMockParityTest;
import org.mule.examples.springboot.parity.ParityFixture;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@code showFormFlow} (GET /) and {@code salesforceDataRetrievalFlow} (POST /) against the WireMock
 * Salesforce API: every fixture asserts the HTTP response plus the exact SOQL / REST calls.
 */
class SalesforceRetrievalParityTest extends AbstractWireMockParityTest {

    @Autowired
    SalesforcePasswordSession session;

    @Override
    protected String fixtureDirectory() {
        return "parity/salesforce-data-retrieval";
    }

    @Override
    protected void beforeCase(ParityFixture fixture) {
        session.invalidate();
        SalesforceParitySupport.stub(DOWNSTREAM, fixture);
    }
}
