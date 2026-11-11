package org.mule.examples.springboot.proxyingasoapapi;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * The checks {@code cxf:proxy-service payload="envelope"} applies before proxying: the body must be a
 * well-formed SOAP 1.1 {@code Envelope} with a {@code Body}. Failures become a SOAP 1.1 {@code soap:Client} fault.
 */
final class SoapEnvelopes {

    static final String SOAP11_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    private SoapEnvelopes() {
    }

    /** @return {@code null} when the payload is a valid SOAP 1.1 envelope, otherwise the fault reason */
    static String invalidReason(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "Message part was empty: no SOAP envelope";
        }
        Element root;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            root = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(payload)).getDocumentElement();
        } catch (Exception e) {
            return "Error reading XMLStreamReader: " + e.getMessage();
        }
        if (!SOAP11_NS.equals(root.getNamespaceURI()) || !"Envelope".equals(root.getLocalName())) {
            return "Message part {" + root.getNamespaceURI() + "}" + root.getLocalName() + " is not a SOAP 1.1 Envelope";
        }
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && SOAP11_NS.equals(e.getNamespaceURI()) && "Body".equals(e.getLocalName())) {
                return null;
            }
        }
        return "SOAP Envelope has no Body";
    }

    static byte[] fault(String code, String reason) {
        String xml = "<soap:Envelope xmlns:soap=\"" + SOAP11_NS + "\"><soap:Body><soap:Fault>"
                + "<faultcode>soap:" + code + "</faultcode><faultstring>" + escape(reason) + "</faultstring>"
                + "</soap:Fault></soap:Body></soap:Envelope>";
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
