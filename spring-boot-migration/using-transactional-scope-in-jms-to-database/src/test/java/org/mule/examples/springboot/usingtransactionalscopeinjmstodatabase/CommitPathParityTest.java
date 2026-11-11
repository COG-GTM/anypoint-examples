package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

import org.springframework.test.context.TestPropertySource;

/** The same flow with the TestComponent not throwing: insert and the "out" message commit together. */
@TestPropertySource(properties = "orders.throw-after-insert=false")
class CommitPathParityTest extends AbstractTransactionalScopeParityTest {

    @Override
    protected String fixtureSubdirectory() {
        return "commit-path";
    }
}
