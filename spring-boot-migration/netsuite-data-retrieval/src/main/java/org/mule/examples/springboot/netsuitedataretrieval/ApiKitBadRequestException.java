package org.mule.examples.springboot.netsuitedataretrieval;

/** Equivalent of APIkit's {@code BadRequestException} raised when a query parameter violates the RAML. */
public class ApiKitBadRequestException extends RuntimeException {

    public ApiKitBadRequestException(String message) {
        super(message);
    }
}
