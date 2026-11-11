package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

class RollbackParityTest extends AbstractTransactionalScopeParityTest {

    @Override
    protected String fixtureSubdirectory() {
        return "rollback";
    }
}
