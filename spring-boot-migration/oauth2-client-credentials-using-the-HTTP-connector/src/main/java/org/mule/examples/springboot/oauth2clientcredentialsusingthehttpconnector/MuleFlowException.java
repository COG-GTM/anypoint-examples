package org.mule.examples.springboot.oauth2clientcredentialsusingthehttpconnector;

/** A failure inside a flow; the HTTP listener answers it with 500 and the message as body. */
public class MuleFlowException extends RuntimeException {

    public MuleFlowException(String message) {
        super(message);
    }

    public MuleFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
