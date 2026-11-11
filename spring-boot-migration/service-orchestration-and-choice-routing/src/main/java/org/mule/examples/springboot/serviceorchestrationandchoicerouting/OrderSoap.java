package org.mule.examples.springboot.serviceorchestrationandchoicerouting;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The {@code cxf:jaxws-service serviceClass="IProcessOrder"} contract: {@code ord:processOrder/order} in,
 * {@code ord:processOrderResponse/summary} out (doc/literal wrapped, unqualified children, JAXB element order).
 */
public final class OrderSoap {

    public static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String ORDERS_NS = "http://orders.se.mulesoft.com/";

    private OrderSoap() {
    }

    public static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid XML: " + e.getMessage(), e);
        }
    }

    public static Model.Order parseRequest(String soap) {
        Element order = child(first(parseXml(soap).getDocumentElement(), "processOrder"), "order");
        if (order == null) {
            throw new IllegalArgumentException("No processOrder/order element");
        }
        Element customer = child(order, "customer");
        List<Model.OrderItem> items = new ArrayList<>();
        Element wrapper = child(order, "orderItems");
        if (wrapper != null) {
            for (Element item : children(wrapper, "item")) {
                String quantity = text(item, "quantity");
                items.add(new Model.OrderItem(text(item, "productId"), text(item, "name"), text(item, "manufacturer"),
                        quantity == null ? 0 : Integer.parseInt(quantity.trim())));
            }
        }
        return new Model.Order(text(order, "orderId"),
                customer == null ? null : new Model.OrderPerson(text(customer, "address"), text(customer, "firstName"),
                        text(customer, "lastName")), items);
    }

    public static String renderResponse(Model.Order order) {
        StringBuilder b = new StringBuilder("<soap:Envelope xmlns:soap=\"" + SOAP_NS + "\"><soap:Body>")
                .append("<ns2:processOrderResponse xmlns:ns2=\"" + ORDERS_NS + "\"><summary>");
        el(b, "orderId", order.orderId());
        if (order.customer() != null) {
            b.append("<customer>");
            el(b, "address", order.customer().address());
            el(b, "firstName", order.customer().firstName());
            el(b, "lastName", order.customer().lastName());
            b.append("</customer>");
        }
        b.append("<orderItems>");
        for (Model.OrderItem item : order.orderItems()) {
            b.append("<item>");
            el(b, "manufacturer", item.manufacturer);
            el(b, "name", item.name);
            el(b, "productId", item.productId);
            if (item.purchaseReceipt != null) {
                b.append("<purchaseReceipt>");
                el(b, "id", item.purchaseReceipt.id());
                el(b, "status", String.valueOf(item.purchaseReceipt.status()));
                el(b, "totalPrice", Float.toString(item.purchaseReceipt.totalPrice()));
                b.append("</purchaseReceipt>");
            }
            el(b, "quantity", Integer.toString(item.quantity));
            b.append("</item>");
        }
        return b.append("</orderItems></summary></ns2:processOrderResponse></soap:Body></soap:Envelope>").toString();
    }

    public static String fault(String message) {
        StringBuilder b = new StringBuilder("<soap:Envelope xmlns:soap=\"" + SOAP_NS + "\"><soap:Body><soap:Fault>")
                .append("<faultcode>soap:Server</faultcode>");
        el(b, "faultstring", message);
        return b.append("</soap:Fault></soap:Body></soap:Envelope>").toString();
    }

    static void el(StringBuilder b, String name, String value) {
        if (value != null) {
            b.append('<').append(name).append('>').append(escape(value)).append("</").append(name).append('>');
        }
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static Element first(Element root, String localName) {
        NodeList list = root.getElementsByTagNameNS("*", localName);
        return list.getLength() == 0 ? null : (Element) list.item(0);
    }

    static Element child(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        List<Element> c = children(parent, localName);
        return c.isEmpty() ? null : c.get(0);
    }

    static List<Element> children(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && localName.equals(e.getLocalName())) {
                result.add(e);
            }
        }
        return result;
    }

    static String text(Element parent, String localName) {
        Element e = child(parent, localName);
        return e == null ? null : e.getTextContent();
    }
}
