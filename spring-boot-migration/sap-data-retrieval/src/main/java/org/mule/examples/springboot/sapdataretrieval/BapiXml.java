package org.mule.examples.springboot.sapdataretrieval;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/** The SAP connector's {@code xmlVersion="2"} document format: {@code <FUNCTION><import/><tables/></FUNCTION>}. */
final class BapiXml {

    private BapiXml() {
    }

    /** Writes {@code <function><import>…</import><tables><table><row>…</row></table></tables></function>}. */
    static String write(String function, Map<String, String> imports, String table, Map<String, String> row) {
        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter w = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement(function);
            w.writeStartElement("import");
            fields(w, imports);
            w.writeEndElement();
            if (table != null) {
                w.writeStartElement("tables");
                w.writeStartElement(table);
                w.writeStartElement("row");
                fields(w, row);
                w.writeEndElement();
                w.writeEndElement();
                w.writeEndElement();
            }
            w.writeEndElement();
            w.writeEndDocument();
            w.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }
        return out.toString();
    }

    static Map<String, String> fields(String... nameValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < nameValues.length; i += 2) {
            map.put(nameValues[i], nameValues[i + 1]);
        }
        return map;
    }

    private static void fields(XMLStreamWriter w, Map<String, String> fields) throws XMLStreamException {
        for (Map.Entry<String, String> f : fields.entrySet()) {
            if (f.getValue() == null) {
                w.writeEmptyElement(f.getKey());
            } else {
                w.writeStartElement(f.getKey());
                w.writeCharacters(f.getValue());
                w.writeEndElement();
            }
        }
    }

    /** Rows of {@code /<function>/tables/<table>/row}; empty if the table is missing or has no rows. */
    /**
     * Parses the raw reply bytes. A charset from the HTTP {@code Content-Type} wins (RFC 7303); otherwise the XML
     * declaration's encoding (e.g. {@code windows-1252}) is honoured, defaulting to UTF-8.
     */
    static List<SapRow> rows(byte[] xml, Charset httpCharset, String table) {
        List<SapRow> rows = new ArrayList<>();
        Element tables = child(parse(xml, httpCharset).getDocumentElement(), "tables");
        Element tableElement = child(tables, table);
        if (tableElement == null) {
            return rows;
        }
        for (Node n = tableElement.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element row && "row".equals(row.getTagName())) {
                SapRow r = new SapRow();
                for (Node f = row.getFirstChild(); f != null; f = f.getNextSibling()) {
                    if (f instanceof Element field) {
                        r.put(field.getTagName(), field.getTextContent());
                    }
                }
                rows.add(r);
            }
        }
        return rows;
    }

    private static InputSource source(byte[] xml, Charset httpCharset) {
        InputSource source = new InputSource(new ByteArrayInputStream(xml));
        if (httpCharset != null) {
            source.setEncoding(httpCharset.name());
        }
        return source;
    }

    private static Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && name.equals(e.getTagName())) {
                return e;
            }
        }
        return null;
    }

    private static Document parse(byte[] xml, Charset httpCharset) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(source(xml, httpCharset));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid BAPI response XML: " + e.getMessage(), e);
        }
    }
}
