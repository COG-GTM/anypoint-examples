package org.ordermgmt.fulfillment.web;

import org.ordermgmt.fulfillment.soap.InvalidSoapRequestException;
import org.ordermgmt.fulfillment.soap.ShippingOrderSoapWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Equivalent of the Mule default exception strategy in front of a CXF service: every failure is
 * rendered as a SOAP fault instead of a Spring Boot JSON error body. Unreadable requests are
 * client faults (HTTP 400), anything else is a server fault (HTTP 500).
 */
@RestControllerAdvice
public class SoapFaultExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SoapFaultExceptionHandler.class);

    private final ShippingOrderSoapWriter writer;

    public SoapFaultExceptionHandler(ShippingOrderSoapWriter writer) {
        this.writer = writer;
    }

    @ExceptionHandler(InvalidSoapRequestException.class)
    public ResponseEntity<String> handleInvalidRequest(InvalidSoapRequestException e) {
        log.warn("Rejecting malformed fulfillment request: {}", e.getMessage());
        return fault(HttpStatus.BAD_REQUEST, "soap:Client", e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return fault(HttpStatus.METHOD_NOT_ALLOWED, "soap:Client", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception e) {
        log.error("Fulfillment request failed", e);
        return fault(HttpStatus.INTERNAL_SERVER_ERROR, "soap:Server", "Order could not be processed");
    }

    private ResponseEntity<String> fault(HttpStatus status, String faultCode, String faultString) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_XML)
                .body(writer.writeFault(faultCode, faultString));
    }
}
