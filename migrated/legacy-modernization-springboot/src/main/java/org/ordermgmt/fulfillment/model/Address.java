package org.ordermgmt.fulfillment.model;

/**
 * Postal address, mirroring {@code org.ordermgmt.Address} of the Mule application.
 */
public record Address(
        String name,
        String line1,
        String line2,
        String city,
        String stateOrProvinceCode,
        String countryCode,
        String postalCode) {
}
