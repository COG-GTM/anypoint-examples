package org.mule.examples.springboot.soapwebservicesecurity;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.util.List;

/**
 * Port of Mule's {@code SAMLCallbackHandler}: a SAML 2.0 sender-vouches assertion for subject
 * {@code AllowGreetingServices} with a {@code Password} authentication statement. The issuer comes from Mule's
 * {@code saml.properties} ({@code self}) or {@code wrong-saml.properties} ({@code www.example.com}).
 */
public class SamlCallbackHandler implements CallbackHandler {

    private final String issuer;
    private final Crypto issuerCrypto;

    /** {@code saml.properties}: issuer, issuer.cryptoProp.file=wssecurity.properties, key joe/secret. */
    public SamlCallbackHandler(String issuer, Crypto issuerCrypto) {
        this.issuer = issuer;
        this.issuerCrypto = issuerCrypto;
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback c : callbacks) {
            if (!(c instanceof SAMLCallback callback)) {
                throw new UnsupportedCallbackException(c, "Unrecognized Callback");
            }
            callback.setSamlVersion(Version.SAML_20);
            callback.setIssuer(issuer);
            if (issuerCrypto != null) {
                callback.setIssuerCrypto(issuerCrypto);
                callback.setIssuerKeyName("joe");
                callback.setIssuerKeyPassword("secret");
            }
            callback.setSubject(new SubjectBean("AllowGreetingServices", "www.example.com", SAML2Constants.CONF_SENDER_VOUCHES));
            AuthenticationStatementBean auth = new AuthenticationStatementBean();
            auth.setAuthenticationMethod("Password");
            callback.setAuthenticationStatementData(List.of(auth));
        }
    }
}
