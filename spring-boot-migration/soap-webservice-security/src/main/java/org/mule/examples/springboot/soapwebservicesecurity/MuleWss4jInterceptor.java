package org.mule.examples.springboot.soapwebservicesecurity;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.security.WsSecurityValidationException;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.soap11.Soap11Body;

import javax.xml.namespace.QName;

/**
 * WSS4J interceptor that reports validation failures like Mule's CXF {@code WSS4JInInterceptor}: a SOAP 1.1 fault
 * whose faultcode is the WSS4J fault QName ({@code wsse:FailedAuthentication}, {@code wsse:InvalidSecurity}) and
 * whose faultstring is the WSS4J message. Failed authentication keeps its own message; any other failure (missing
 * action, rejected SAML assertion) is reported as {@code InvalidSecurity}.
 */
class MuleWss4jInterceptor extends Wss4jSecurityInterceptor {

    private boolean validateSamlSubjectConfirmation = true;

    /**
     * WSS4J 2 rejects an unsigned sender-vouches assertion by default; the WSS4J 1.6 stack in Mule 3.8 accepted it
     * (SamlTokenServiceFlow / SoapWebserviceSecurityIT#testSamlToken), so the SAML flows switch the check off.
     */
    void setValidateSamlSubjectConfirmation(boolean validate) {
        this.validateSamlSubjectConfirmation = validate;
    }

    @Override
    protected RequestData initializeValidationRequestData(MessageContext messageContext) {
        RequestData data = super.initializeValidationRequestData(messageContext);
        data.setValidateSamlSubjectConfirmation(validateSamlSubjectConfirmation);
        return data;
    }

    @Override
    protected boolean handleValidationException(WsSecurityValidationException ex, MessageContext messageContext) {
        WSSecurityException wss = wssCause(ex);
        WSSecurityException reported = wss != null && wss.getErrorCode() == WSSecurityException.ErrorCode.FAILED_AUTHENTICATION
                ? wss : new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        QName code = reported.getFaultCode();
        Soap11Body body = (Soap11Body) ((SoapMessage) messageContext.getResponse()).getSoapBody();
        body.addFault(code, reported.getMessage(), null);
        return false;
    }

    private static WSSecurityException wssCause(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof WSSecurityException w) {
                return w;
            }
        }
        return null;
    }
}
