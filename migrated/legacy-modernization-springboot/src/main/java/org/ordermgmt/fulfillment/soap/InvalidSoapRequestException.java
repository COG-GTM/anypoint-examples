package org.ordermgmt.fulfillment.soap;

/**
 * Raised when the inbound envelope cannot be read as a {@code putShippingOrder} request.
 * Maps to a {@code soap:Client} fault, the CXF equivalent of a JAXB unmarshalling failure.
 */
public class InvalidSoapRequestException extends RuntimeException {

    public InvalidSoapRequestException(String message) {
        super(message);
    }

    public InvalidSoapRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
