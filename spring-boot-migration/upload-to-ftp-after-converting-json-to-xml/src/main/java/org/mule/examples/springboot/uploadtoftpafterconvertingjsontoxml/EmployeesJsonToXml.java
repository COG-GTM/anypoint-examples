package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import com.fasterxml.jackson.databind.JsonNode;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * Java version of the "JSON to XML" DataWeave script in {@code upload-to-ftp.xml}:
 * <pre>
 * employees: { (payload.employees.employee map {
 *     employee: { name: $.name, lastName: $.lastName,
 *                 addresses: { ($.addresses.address map { address: $ }) } } }) }
 * </pre>
 */
public final class EmployeesJsonToXml {

    static final String XML_DECLARATION = "<?xml version='1.0' encoding='UTF-8'?>";
    private static final XMLOutputFactory XML = XMLOutputFactory.newFactory();

    private EmployeesJsonToXml() {
    }

    public static String transform(JsonNode payload) {
        StringWriter out = new StringWriter();
        out.write(XML_DECLARATION);
        out.write('\n');
        try {
            XMLStreamWriter xml = XML.createXMLStreamWriter(out);
            xml.writeStartElement("employees");
            for (JsonNode employee : elements(payload.path("employees").path("employee"))) {
                indent(xml, 1);
                xml.writeStartElement("employee");
                textElement(xml, 2, "name", employee.get("name"));
                textElement(xml, 2, "lastName", employee.get("lastName"));
                indent(xml, 2);
                xml.writeStartElement("addresses");
                boolean any = false;
                for (JsonNode address : elements(employee.path("addresses").path("address"))) {
                    any = true;
                    indent(xml, 3);
                    xml.writeStartElement("address");
                    for (Iterator<Map.Entry<String, JsonNode>> it = address.fields(); it.hasNext(); ) {
                        Map.Entry<String, JsonNode> field = it.next();
                        textElement(xml, 4, field.getKey(), field.getValue());
                    }
                    indent(xml, 3);
                    xml.writeEndElement();
                }
                if (any) {
                    indent(xml, 2);
                }
                xml.writeEndElement();
                indent(xml, 1);
                xml.writeEndElement();
            }
            indent(xml, 0);
            xml.writeEndElement();
            xml.flush();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Could not write employees XML", e);
        }
        return out.toString();
    }

    /** DataWeave's {@code map} treats a single object like a one-element array and a missing value like an empty one. */
    private static Iterable<JsonNode> elements(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return java.util.List.of();
        }
        return node.isArray() ? node : java.util.List.of(node);
    }

    private static void textElement(XMLStreamWriter xml, int depth, String name, JsonNode value) throws XMLStreamException {
        indent(xml, depth);
        if (value == null || value.isNull() || value.isMissingNode()) {
            xml.writeEmptyElement(name);
            return;
        }
        xml.writeStartElement(name);
        xml.writeCharacters(value.isValueNode() ? value.asText() : value.toString());
        xml.writeEndElement();
    }

    private static void indent(XMLStreamWriter xml, int depth) throws XMLStreamException {
        xml.writeCharacters("\n" + "  ".repeat(depth));
    }
}
