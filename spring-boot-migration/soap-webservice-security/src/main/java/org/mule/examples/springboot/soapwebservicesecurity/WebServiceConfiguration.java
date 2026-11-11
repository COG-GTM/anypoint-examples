package org.mule.examples.springboot.soapwebservicesecurity;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.server.endpoint.MethodEndpoint;
import org.springframework.ws.server.endpoint.mapping.AbstractEndpointMapping;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.adapter.method.MethodArgumentResolver;
import org.springframework.ws.server.endpoint.adapter.method.MethodReturnValueHandler;
import org.springframework.ws.server.endpoint.interceptor.EndpointInterceptorAdapter;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import java.util.List;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

import java.util.Properties;

/**
 * The six Mule flows of {@code mule-config.xml}: one {@code http:listener path} under basePath {@code services} each,
 * all backed by {@link GreeterEndpoint}, with the flow's {@code cxf:ws-security} config as a path-scoped WSS4J
 * interceptor.
 */
@Configuration
@EnableWs
class WebServiceConfiguration implements WsConfigurer {

    static final String[] PATHS = {"unsecure", "username", "signed", "encrypted", "saml", "signedsaml"};

    @Bean
    ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        String[] mappings = new String[PATHS.length];
        for (int i = 0; i < PATHS.length; i++) {
            mappings[i] = "/services/" + PATHS[i];
        }
        return new ServletRegistrationBean<>(servlet, mappings);
    }

    /** Mule {@code wssecurity.properties}: Merlin crypto over the original {@code keystore.jks}. */
    @Bean
    Crypto wssCrypto(@Value("${security.keystore.file}") String file,
                     @Value("${security.keystore.password}") String password,
                     @Value("${security.keystore.alias}") String alias) throws WSSecurityException {
        Properties props = new Properties();
        props.setProperty("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
        props.setProperty("org.apache.wss4j.crypto.merlin.keystore.type", "jks");
        props.setProperty("org.apache.wss4j.crypto.merlin.keystore.password", password);
        props.setProperty("org.apache.wss4j.crypto.merlin.keystore.alias", alias);
        props.setProperty("org.apache.wss4j.crypto.merlin.keystore.file", file);
        return CryptoFactory.getInstance(props);
    }

    /** UsernameTokenServiceFlow: {@code action="UsernameToken Timestamp"}. */
    @Bean
    PathScopedInterceptor usernameSecurity() {
        MuleWss4jInterceptor i = base("UsernameToken Timestamp");
        return new PathScopedInterceptor("/services/username", i);
    }

    /** UsernameTokenSignedServiceFlow: {@code action="UsernameToken Signature Timestamp"}, signaturePropFile. */
    @Bean
    PathScopedInterceptor signedSecurity(Crypto wssCrypto) {
        MuleWss4jInterceptor i = base("UsernameToken Signature Timestamp");
        i.setValidationSignatureCrypto(wssCrypto);
        return new PathScopedInterceptor("/services/signed", i);
    }

    /** UsernameTokenEncryptedServiceFlow: {@code action="UsernameToken Timestamp Encrypt"}, decryptionPropFile. */
    @Bean
    PathScopedInterceptor encryptedSecurity(Crypto wssCrypto) {
        MuleWss4jInterceptor i = base("UsernameToken Timestamp Encrypt");
        i.setValidationDecryptionCrypto(wssCrypto);
        return new PathScopedInterceptor("/services/encrypted", i);
    }

    /** SamlTokenServiceFlow: {@code action="SAMLTokenUnsigned Timestamp"} + {@code saml2-token-validator}. */
    @Bean
    PathScopedInterceptor samlSecurity() {
        MuleWss4jInterceptor i = base("SAMLTokenUnsigned Timestamp");
        i.setWssConfig(samlConfig());
        i.setValidateSamlSubjectConfirmation(false);
        return new PathScopedInterceptor("/services/saml", i);
    }

    /** SignedSamlTokenServiceFlow: {@code action="SAMLTokenUnsigned Signature"} + validator, signaturePropFile. */
    @Bean
    PathScopedInterceptor signedSamlSecurity(Crypto wssCrypto) {
        MuleWss4jInterceptor i = base("SAMLTokenUnsigned Signature");
        i.setValidationSignatureCrypto(wssCrypto);
        i.setWssConfig(samlConfig());
        i.setValidateSamlSubjectConfirmation(false);
        return new PathScopedInterceptor("/services/signedsaml", i);
    }

    /**
     * Mule dispatches by listener path, not by payload. An encrypted body has no {@code greet} root until WSS4J has
     * decrypted it, so this mapping routes any request on a service path to the greeter when payload-root mapping
     * finds nothing.
     */
    @Bean
    PathEndpointMapping pathEndpointMapping(GreeterEndpoint greeterEndpoint) throws NoSuchMethodException {
        PathEndpointMapping mapping = new PathEndpointMapping(
                new MethodEndpoint(greeterEndpoint, GreeterEndpoint.class.getDeclaredMethod("greet", org.w3c.dom.Element.class)));
        mapping.setInterceptors(new EndpointInterceptor[] {new EmptyHeaderRemover()});
        return mapping;
    }

    static final class PathEndpointMapping extends AbstractEndpointMapping {

        private final MethodEndpoint endpoint;

        PathEndpointMapping(MethodEndpoint endpoint) {
            this.endpoint = endpoint;
            setOrder(Ordered.LOWEST_PRECEDENCE);
        }

        @Override
        protected Object getEndpointInternal(MessageContext messageContext) throws Exception {
            TransportContext context = TransportContextHolder.getTransportContext();
            return context != null && context.getConnection().getUri().getPath().startsWith("/services/") ? endpoint : null;
        }
    }

    private static MuleWss4jInterceptor base(String actions) {
        MuleWss4jInterceptor i = new MuleWss4jInterceptor();
        i.setValidationActions(actions);
        i.setValidationCallbackHandler(new PasswordCallback());
        return i;
    }

    private static WSSConfig samlConfig() {
        WSSConfig config = WSSConfig.getNewInstance();
        config.setValidator(WSConstants.SAML2_TOKEN, new SamlCustomValidator());
        return config;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(new EmptyHeaderRemover());
    }

    @Override
    public void addArgumentResolvers(List<MethodArgumentResolver> argumentResolvers) {
    }

    @Override
    public void addReturnValueHandlers(List<MethodReturnValueHandler> returnValueHandlers) {
    }

    /** CXF (Mule) writes no {@code soap:Header} when it is empty; SAAJ always adds one. */
    static final class EmptyHeaderRemover extends EndpointInterceptorAdapter {

        @Override
        public boolean handleResponse(MessageContext messageContext, Object endpoint) {
            strip(messageContext);
            return true;
        }

        @Override
        public boolean handleFault(MessageContext messageContext, Object endpoint) {
            strip(messageContext);
            return true;
        }

        @Override
        public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
            strip(messageContext);
        }

        private static void strip(MessageContext messageContext) {
            if (messageContext.hasResponse() && messageContext.getResponse() instanceof SaajSoapMessage saaj) {
                try {
                    SOAPHeader header = saaj.getSaajMessage().getSOAPHeader();
                    if (header != null && !header.hasChildNodes()) {
                        header.detachNode();
                    }
                } catch (SOAPException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}
