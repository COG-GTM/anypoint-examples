package org.mule.examples.springboot.addinganewcustomertoworkdayrevenuemanagement;

/**
 * Typed Workday {@code Put_Customer_Request} (the fields the Mule DataWeave "XML to Workday PutCustomerRequest XML"
 * populates).
 */
public record PutCustomerRequest(
        String customerId,
        String customerReferenceId,
        String customerName,
        String businessEntityName,
        String categoryReferenceType,
        String categoryReferenceValue,
        String statusReferenceType,
        String statusReferenceValue) {
}
