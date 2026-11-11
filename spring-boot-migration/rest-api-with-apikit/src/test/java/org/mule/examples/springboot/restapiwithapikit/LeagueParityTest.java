package org.mule.examples.springboot.restapiwithapikit;

import org.mule.examples.springboot.parity.AbstractParityTest;

/** Fixtures run in file-name order against one league, like requests against one running Mule app. */
class LeagueParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/rest-api-with-apikit";
    }
}
