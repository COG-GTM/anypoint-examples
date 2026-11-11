package org.mule.examples.springboot.soapwebservicesecurity;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.SmartEndpointInterceptor;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.SoapEndpointInterceptor;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;

import java.net.URISyntaxException;

/** Applies a WS-Security interceptor only to one Mule {@code http:listener path} (one flow per path in Mule). */
class PathScopedInterceptor implements SmartEndpointInterceptor, SoapEndpointInterceptor {

    private final String path;
    private final MuleWss4jInterceptor delegate;

    PathScopedInterceptor(String path, MuleWss4jInterceptor delegate) {
        this.path = path;
        this.delegate = delegate;
    }

    @Override
    public boolean shouldIntercept(MessageContext messageContext, Object endpoint) {
        TransportContext context = TransportContextHolder.getTransportContext();
        try {
            return context != null && path.equals(context.getConnection().getUri().getPath());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public boolean understands(SoapHeaderElement header) {
        return delegate.understands(header);
    }

    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
        return delegate.handleRequest(messageContext, endpoint);
    }

    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
        return delegate.handleResponse(messageContext, endpoint);
    }

    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
        return delegate.handleFault(messageContext, endpoint);
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) throws Exception {
        delegate.afterCompletion(messageContext, endpoint, ex);
    }
}
