package org.mule.examples.springboot.munitshorttutorial;

import org.mule.examples.springboot.parity.AbstractParityTest;

class MunitShortTutorialParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/munit-short-tutorial";
    }
}
