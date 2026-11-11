package org.mule.examples.springboot.processingorderswithdataweaveandapikit;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Reads the {@code orders/item} structure of orders.xsd into {@link OrderMapper.Item}s. */
final class OrdersXmlReader {

    private OrdersXmlReader() {
    }

    static List<OrderMapper.Item> read(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element orders = factory.newDocumentBuilder().parse(file).getDocumentElement();
        List<OrderMapper.Item> items = new ArrayList<>();
        for (Element item : children(orders, "item")) {
            Element properties = first(item, "properties");
            List<String> authors = new ArrayList<>();
            Element authorsElement = properties == null ? null : first(properties, "authors");
            if (authorsElement != null) {
                for (Element author : children(authorsElement, "author")) {
                    authors.add(author.getTextContent().trim());
                }
            }
            items.add(new OrderMapper.Item(decimal(first(item, "price")),
                    properties == null ? null : text(first(properties, "title")), authors,
                    properties == null ? null : decimal(first(properties, "year"))));
        }
        return items;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n instanceof Element e && name.equals(e.getTagName())) {
                result.add(e);
            }
        }
        return result;
    }

    private static Element first(Element parent, String name) {
        List<Element> c = children(parent, name);
        return c.isEmpty() ? null : c.get(0);
    }

    private static String text(Element e) {
        return e == null ? null : e.getTextContent().trim();
    }

    private static BigDecimal decimal(Element e) {
        return e == null ? null : new BigDecimal(e.getTextContent().trim());
    }
}
