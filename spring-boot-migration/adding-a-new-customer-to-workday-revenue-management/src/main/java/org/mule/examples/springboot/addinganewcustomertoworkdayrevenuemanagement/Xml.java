package org.mule.examples.springboot.addinganewcustomertoworkdayrevenuemanagement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

final class Xml {

    private Xml() {
    }

    static Document parse(String xml, boolean namespaceAware) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(namespaceAware);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid XML: " + e.getMessage(), e);
        }
    }
}
