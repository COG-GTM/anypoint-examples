package org.ordermgmt.fulfillment.soap;

/**
 * XML names used on the wire by the CXF {@code jaxws-service} of the Mule application.
 */
public final class SoapConstants {

    public static final String SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String ORDER_MGMT_NS = "http://ordermgmt.org/";
    public static final String PUT_SHIPPING_ORDER = "putShippingOrder";
    public static final String PUT_SHIPPING_ORDER_RESPONSE = "putShippingOrderResponse";

    private SoapConstants() {
    }
}
