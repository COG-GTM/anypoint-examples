package org.mule.examples.springboot.soapwebservicesecurity;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SamlAssertionValidator;

import java.util.List;

/**
 * Port of Mule's {@code SAMLCustomValidator} ({@code cxf:saml2-token-validator}): issuer {@code self}, SAML 2.0,
 * sender-vouches confirmation and subject {@code AllowGreetingServices}.
 */
public class SamlCustomValidator extends SamlAssertionValidator {

    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        Credential returned = super.validate(credential, data);
        SamlAssertionWrapper assertion = credential.getSamlAssertion();
        if (!"self".equals(assertion.getIssuerString())) {
            throw invalid();
        }
        if (assertion.getSaml2() == null) {
            throw invalid();
        }
        List<String> methods = assertion.getConfirmationMethods();
        String confirmationMethod = methods.isEmpty() ? null : methods.get(0);
        if (confirmationMethod == null || !OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
            throw invalid();
        }
        if (!"AllowGreetingServices".equals(assertion.getSaml2().getSubject().getNameID().getValue())) {
            throw invalid();
        }
        return returned;
    }

    private static WSSecurityException invalid() {
        return new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
    }
}
