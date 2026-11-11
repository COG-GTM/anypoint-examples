package org.mule.examples.springboot.soapwebservicesecurity;

import org.apache.wss4j.common.crypto.Crypto;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;

import java.util.Optional;

/**
 * The client side {@code cxf:ws-security} configs of the Mule sub-flows in {@code service-clients.xml}, as Spring-WS
 * client interceptors. Also used by the parity tests to build the WS-Security headers a Mule/CXF client sends.
 */
public final class SecurityClients {

    private SecurityClients() {
    }

    /** Secured path and client interceptor (empty for {@code unsecure}) for a Mule {@code clientType}. */
    public record Client(String path, Optional<Wss4jSecurityInterceptor> interceptor) {
    }

    public static Optional<Client> forType(String clientType, Crypto crypto) {
        return Optional.ofNullable(switch (clientType == null ? "" : clientType) {
            case "unsecure" -> new Client("unsecure", Optional.empty());
            case "usernameToken" -> new Client("username", Optional.of(usernameToken("UsernameToken Timestamp", "secret")));
            case "usernameTokenSigned" -> new Client("signed", Optional.of(usernameTokenSigned(crypto)));
            case "usernameTokenEncrypted" -> new Client("encrypted", Optional.of(usernameTokenEncrypted(crypto)));
            case "samlToken" -> new Client("saml", Optional.of(samlToken("self", crypto)));
            case "samlTokenSigned" -> new Client("signedsaml", Optional.of(samlTokenSigned(crypto)));
            default -> null;
        });
    }

    /** {@code action} with {@code user=joe} and the password {@code PasswordCallback} gives joe. */
    public static Wss4jSecurityInterceptor usernameToken(String actions, String password) {
        Wss4jSecurityInterceptor i = new Wss4jSecurityInterceptor();
        i.setSecurementActions(actions);
        i.setSecurementUsername("joe");
        i.setSecurementPassword(password);
        return i;
    }

    /** {@code UsernameToken Signature Timestamp}, signaturePropFile wssecurity.properties. */
    public static Wss4jSecurityInterceptor usernameTokenSigned(Crypto crypto) {
        Wss4jSecurityInterceptor i = usernameToken("UsernameToken Signature Timestamp", "secret");
        i.setSecurementSignatureCrypto(crypto);
        i.setSecurementSignatureUser("joe");
        return i;
    }

    /** {@code UsernameToken Timestamp Encrypt}, encryptionPropFile wssecurity.properties, encryptionUser joe. */
    public static Wss4jSecurityInterceptor usernameTokenEncrypted(Crypto crypto) {
        Wss4jSecurityInterceptor i = usernameToken("UsernameToken Timestamp Encrypt", "secret");
        i.setSecurementEncryptionCrypto(crypto);
        i.setSecurementEncryptionUser("joe");
        return i;
    }

    /** {@code SAMLTokenUnsigned Timestamp} with {@code SAMLCallbackHandler} and the given issuer. */
    public static Wss4jSecurityInterceptor samlToken(String issuer, Crypto crypto) {
        Wss4jSecurityInterceptor i = new Wss4jSecurityInterceptor();
        i.setSecurementActions("SAMLTokenUnsigned Timestamp");
        i.setSecurementSamlCallbackHandler(new SamlCallbackHandler(issuer, crypto));
        return i;
    }

    /** {@code SAMLTokenSigned}, signatureKeyIdentifier DirectReference, user joe. */
    public static Wss4jSecurityInterceptor samlTokenSigned(Crypto crypto) {
        Wss4jSecurityInterceptor i = new Wss4jSecurityInterceptor();
        i.setSecurementActions("SAMLTokenSigned");
        i.setSecurementSamlCallbackHandler(new SamlCallbackHandler("self", crypto));
        i.setSecurementSignatureCrypto(crypto);
        i.setSecurementSignatureUser("joe");
        i.setSecurementPassword("secret");
        i.setSecurementSignatureKeyIdentifier("DirectReference");
        return i;
    }
}
