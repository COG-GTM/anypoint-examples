package org.mule.examples.springboot.xmlonlysoapwebservice;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.interceptor.EndpointInterceptorAdapter;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import java.util.List;

/** The three Mule {@code http:listener} paths, all served by one Spring-WS dispatcher. */
@Configuration
@EnableWs
class WebServiceConfiguration implements WsConfigurer {

    @Bean
    ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        return new ServletRegistrationBean<>(servlet, "/AdmissionService", "/PatientService", "/EHRService");
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(new EmptyHeaderRemover());
    }

    @Override
    public void addArgumentResolvers(List<org.springframework.ws.server.endpoint.adapter.method.MethodArgumentResolver> argumentResolvers) {
    }

    @Override
    public void addReturnValueHandlers(List<org.springframework.ws.server.endpoint.adapter.method.MethodReturnValueHandler> returnValueHandlers) {
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
