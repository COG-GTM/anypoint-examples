package org.mule.examples.springboot.choiceexceptionstrategy;

import org.mule.examples.springboot.parity.AbstractParityTest;

class ChoiceExceptionStrategyParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/implementing-a-choice-exception-strategy";
    }
}
