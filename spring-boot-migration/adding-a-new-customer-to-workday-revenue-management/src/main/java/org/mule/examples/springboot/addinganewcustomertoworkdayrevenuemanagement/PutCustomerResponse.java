package org.mule.examples.springboot.addinganewcustomertoworkdayrevenuemanagement;

/** The part of Workday's {@code Put_Customer_Response} the Mule flow reads: {@code Customer_Reference/@Descriptor}. */
public record PutCustomerResponse(String customerDescriptor) {
}
