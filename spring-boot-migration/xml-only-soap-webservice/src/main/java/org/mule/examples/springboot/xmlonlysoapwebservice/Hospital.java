package org.mule.examples.springboot.xmlonlysoapwebservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** Namespaces and DOM helpers shared by the endpoints (the Mule {@code mulexml:namespace-manager}). */
final class Hospital {

    static final String MSG = "http://www.mule-health.com/SOA/message/1.0";
    static final String MOD = "http://www.mule-health.com/SOA/model/1.0";

    /** DataWeave 1.0 {@code now} / {@code now as :string}. */
    static final DateTimeFormatter DW_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private Hospital() {
    }

    static String now() {
        return OffsetDateTime.now().format(DW_DATETIME);
    }

    static Document newDocument() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Root element {@code ns0:<name>} in the message namespace, with {@code ns1} declared for the model namespace. */
    static Element root(String name) {
        Document doc = newDocument();
        Element root = doc.createElementNS(MSG, "ns0:" + name);
        root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ns1", MOD);
        doc.appendChild(root);
        return root;
    }

    static Element model(Element parent, String name) {
        Element e = parent.getOwnerDocument().createElementNS(MOD, "ns1:" + name);
        parent.appendChild(e);
        return e;
    }

    static Element plain(Element parent, String name, String text) {
        Element e = parent.getOwnerDocument().createElementNS(null, name);
        if (text != null) {
            e.setTextContent(text);
        }
        parent.appendChild(e);
        return e;
    }

    /** First child element with the given local name (DataWeave {@code payload.a.b} ignores namespaces unless given). */
    static Optional<Element> child(Element parent, String localName) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && localName.equals(e.getLocalName())) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    static String text(Element parent, String localName) {
        return child(parent, localName).map(Element::getTextContent).orElse(null);
    }
}
