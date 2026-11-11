package org.mule.examples.springboot.legacymodernization;

import java.util.List;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

/** CXF omits an empty {@code soap:Header} in responses; SAAJ always creates one, so drop it when it is empty. */
@Configuration
public class SoapEnvelopeConfig extends WsConfigurerAdapter {

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(new EndpointInterceptor() {
            @Override
            public boolean handleRequest(MessageContext messageContext, Object endpoint) {
                return true;
            }

            @Override
            public boolean handleResponse(MessageContext messageContext, Object endpoint) {
                removeEmptyHeader(messageContext);
                return true;
            }

            @Override
            public boolean handleFault(MessageContext messageContext, Object endpoint) {
                removeEmptyHeader(messageContext);
                return true;
            }

            @Override
            public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
            }
        });
    }

    private static void removeEmptyHeader(MessageContext messageContext) {
        if (messageContext.hasResponse() && messageContext.getResponse() instanceof SaajSoapMessage message) {
            try {
                SOAPHeader header = message.getSaajMessage().getSOAPHeader();
                if (header != null && !header.hasChildNodes()) {
                    header.detachNode();
                }
            } catch (SOAPException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
