package org.mule.examples.springboot.loginformusingthehttpconnector;

import org.mule.examples.springboot.parity.AbstractParityTest;

class LoginParityTest extends AbstractParityTest {

    @Override
    protected String fixtureDirectory() {
        return "parity/login-form-using-the-http-connector";
    }
}
