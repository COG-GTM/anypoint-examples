package org.ordermgmt.fulfillment.soap;

import static org.ordermgmt.fulfillment.soap.SoapConstants.ORDER_MGMT_NS;
import static org.ordermgmt.fulfillment.soap.SoapConstants.PUT_SHIPPING_ORDER;
import static org.ordermgmt.fulfillment.soap.SoapConstants.SOAP_ENVELOPE_NS;

import java.util.ArrayList;
import java.util.List;
import org.ordermgmt.fulfillment.model.Address;
import org.ordermgmt.fulfillment.model.Order;
import org.ordermgmt.fulfillment.model.OrderItem;
import org.ordermgmt.fulfillment.model.ShippingOrder;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads a {@code putShippingOrder} SOAP request into the domain model. Replaces the JAXB
 * unmarshalling performed by the Mule {@code cxf:jaxws-service} element.
 */
@Component
public class ShippingOrderSoapReader {

    public ShippingOrder read(String soapEnvelope) {
        if (soapEnvelope == null || soapEnvelope.isBlank()) {
            throw new InvalidSoapRequestException("Request body is empty");
        }
        Document document = XmlDocuments.parse(soapEnvelope);
        Element body = firstChild(document.getDocumentElement(), SOAP_ENVELOPE_NS, "Body")
                .orElseThrow(() -> new InvalidSoapRequestException("SOAP Body element is missing"));
        Element request = firstChild(body, ORDER_MGMT_NS, PUT_SHIPPING_ORDER)
                .orElseThrow(() -> new InvalidSoapRequestException(
                        "Unsupported operation: expected a " + PUT_SHIPPING_ORDER + " element"));

        return new ShippingOrder(
                textOf(request, "shippingId"),
                readAddress(request, "billingAddress"),
                readAddress(request, "shippingAddress"),
                readOrder(request));
    }

    private Address readAddress(Element request, String elementName) {
        Element address = firstChild(request, null, elementName).orElse(null);
        if (address == null) {
            return null;
        }
        return new Address(
                textOf(address, "name"),
                textOf(address, "line1"),
                textOf(address, "line2"),
                textOf(address, "city"),
                textOf(address, "stateOrProvinceCode"),
                textOf(address, "countryCode"),
                textOf(address, "postalCode"));
    }

    private Order readOrder(Element request) {
        Element order = firstChild(request, null, "order").orElse(null);
        if (order == null) {
            return new Order(List.of());
        }
        List<OrderItem> items = new ArrayList<>();
        for (Element item : children(order, "orderItem")) {
            items.add(new OrderItem(textOf(item, "merchantSKU"), quantityOf(item)));
        }
        return new Order(items);
    }

    private int quantityOf(Element item) {
        String quantity = textOf(item, "quantity");
        if (quantity == null || quantity.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(quantity.trim());
        } catch (NumberFormatException e) {
            throw new InvalidSoapRequestException("quantity is not a valid integer: " + quantity, e);
        }
    }

    private String textOf(Element parent, String elementName) {
        return firstChild(parent, null, elementName)
                .map(Node::getTextContent)
                .orElse(null);
    }

    private List<Element> children(Element parent, String localName) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && localName.equals(localNameOf(element))) {
                elements.add(element);
            }
        }
        return elements;
    }

    private java.util.Optional<Element> firstChild(Element parent, String namespace, String localName) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element
                    && localName.equals(localNameOf(element))
                    && (namespace == null || namespace.equals(element.getNamespaceURI()))) {
                return java.util.Optional.of(element);
            }
        }
        return java.util.Optional.empty();
    }

    private String localNameOf(Element element) {
        return element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
    }
}
