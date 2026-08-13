package org.ordermgmt.fulfillment.soap;

import static org.ordermgmt.fulfillment.soap.SoapConstants.ORDER_MGMT_NS;
import static org.ordermgmt.fulfillment.soap.SoapConstants.PUT_SHIPPING_ORDER_RESPONSE;
import static org.ordermgmt.fulfillment.soap.SoapConstants.SOAP_ENVELOPE_NS;

import org.ordermgmt.fulfillment.model.Address;
import org.ordermgmt.fulfillment.model.Order;
import org.ordermgmt.fulfillment.model.OrderItem;
import org.ordermgmt.fulfillment.model.ShippingOrder;
import org.ordermgmt.fulfillment.model.ShippingOrderConfirmation;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Writes the {@code putShippingOrderResponse} envelope and SOAP faults. Element names, prefixes
 * and ordering reproduce the CXF output captured in
 * {@code legacy-modernization/src/test/resources/reply.xml}.
 */
@Component
public class ShippingOrderSoapWriter {

    public String writeConfirmation(ShippingOrderConfirmation confirmation) {
        Document document = XmlDocuments.newDocument();
        Element envelope = document.createElementNS(SOAP_ENVELOPE_NS, "soap:Envelope");
        document.appendChild(envelope);
        Element body = document.createElementNS(SOAP_ENVELOPE_NS, "soap:Body");
        envelope.appendChild(body);
        Element response = document.createElementNS(ORDER_MGMT_NS, "ns2:" + PUT_SHIPPING_ORDER_RESPONSE);
        body.appendChild(response);

        Element confirmationElement = document.createElement("ShippingOrderConfirmation");
        response.appendChild(confirmationElement);
        appendShippingOrder(document, confirmationElement, confirmation.shippingOrder());
        appendText(document, confirmationElement, "orderReceivedStatus",
                Boolean.toString(confirmation.orderReceivedStatus()));

        return XmlDocuments.serialize(document);
    }

    public String writeFault(String faultCode, String faultString) {
        Document document = XmlDocuments.newDocument();
        Element envelope = document.createElementNS(SOAP_ENVELOPE_NS, "soap:Envelope");
        document.appendChild(envelope);
        Element body = document.createElementNS(SOAP_ENVELOPE_NS, "soap:Body");
        envelope.appendChild(body);
        Element fault = document.createElementNS(SOAP_ENVELOPE_NS, "soap:Fault");
        body.appendChild(fault);
        appendText(document, fault, "faultcode", faultCode);
        appendText(document, fault, "faultstring", faultString);
        return XmlDocuments.serialize(document);
    }

    private void appendShippingOrder(Document document, Element parent, ShippingOrder shippingOrder) {
        if (shippingOrder == null) {
            return;
        }
        Element element = document.createElement("shippingOrder");
        parent.appendChild(element);
        appendText(document, element, "shippingId", shippingOrder.shippingId());
        appendAddress(document, element, "billingAddress", shippingOrder.billingAddress());
        appendAddress(document, element, "shippingAddress", shippingOrder.shippingAddress());
        appendOrder(document, element, shippingOrder.order());
    }

    private void appendAddress(Document document, Element parent, String elementName, Address address) {
        if (address == null) {
            return;
        }
        Element element = document.createElement(elementName);
        parent.appendChild(element);
        appendText(document, element, "name", address.name());
        appendText(document, element, "line1", address.line1());
        appendText(document, element, "line2", address.line2());
        appendText(document, element, "city", address.city());
        appendText(document, element, "stateOrProvinceCode", address.stateOrProvinceCode());
        appendText(document, element, "countryCode", address.countryCode());
        appendText(document, element, "postalCode", address.postalCode());
    }

    private void appendOrder(Document document, Element parent, Order order) {
        if (order == null) {
            return;
        }
        Element element = document.createElement("order");
        parent.appendChild(element);
        for (OrderItem item : order.orderItemList()) {
            Element itemElement = document.createElement("orderItem");
            element.appendChild(itemElement);
            appendText(document, itemElement, "merchantSKU", item.merchantSKU());
            appendText(document, itemElement, "quantity", Integer.toString(item.quantity()));
        }
    }

    private void appendText(Document document, Element parent, String elementName, String value) {
        if (value == null) {
            return;
        }
        Element element = document.createElement(elementName);
        element.setTextContent(value);
        parent.appendChild(element);
    }
}
