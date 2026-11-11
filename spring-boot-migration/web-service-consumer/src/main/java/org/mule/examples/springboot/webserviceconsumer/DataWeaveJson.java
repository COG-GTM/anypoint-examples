package org.mule.examples.springboot.webserviceconsumer;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The two DataWeave 1.0 conversions of the Mule flows:
 * <ul>
 *   <li>JSON → XML ({@code %output application/xml}, {@code ns0#OrderTshirt: payload}): every field becomes a child
 *       element in document order, arrays become repeated elements.</li>
 *   <li>XML → JSON ({@code %output application/json}): elements become fields in document order, repeated elements
 *       become <b>repeated keys</b> (DataWeave 1.0 objects allow duplicate keys), leaf text stays a string, prefixes
 *       and attributes are dropped, output is indented by two spaces with {@code "key": value}.</li>
 * </ul>
 */
final class DataWeaveJson {

    private DataWeaveJson() {
    }

    static String toXmlElement(String prefix, String namespace, String name, JsonNode payload) {
        StringBuilder out = new StringBuilder();
        out.append('<').append(prefix).append(':').append(name).append(" xmlns:").append(prefix).append("=\"").append(namespace).append("\">");
        writeXmlContent(out, payload);
        out.append("</").append(prefix).append(':').append(name).append('>');
        return out.toString();
    }

    private static void writeXmlContent(StringBuilder out, JsonNode value) {
        if (value.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                List<JsonNode> items = new ArrayList<>();
                if (f.getValue().isArray()) {
                    f.getValue().forEach(items::add);
                } else {
                    items.add(f.getValue());
                }
                for (JsonNode item : items) {
                    out.append('<').append(f.getKey()).append('>');
                    writeXmlContent(out, item);
                    out.append("</").append(f.getKey()).append('>');
                }
            }
        } else if (!value.isNull()) {
            out.append(Xml.escape(value.asText()));
        }
    }

    /** {@code payload} as JSON: an object with the root element as its only key. */
    static String document(Element root) {
        StringBuilder out = new StringBuilder("{\n");
        indent(out, 1).append(quote(root.getLocalName())).append(": ");
        writeValue(out, root, 1);
        return out.append("\n}").toString();
    }

    /** {@code payload.ns0#Root} as JSON: the root element's content. */
    static String content(Element root) {
        StringBuilder out = new StringBuilder();
        writeValue(out, root, 0);
        return out.toString();
    }

    private static void writeValue(StringBuilder out, Element element, int depth) {
        List<Element> children = new ArrayList<>();
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                children.add(e);
            }
        }
        if (children.isEmpty()) {
            out.append(element.hasChildNodes() ? quote(element.getTextContent()) : "null");
            return;
        }
        out.append("{\n");
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            indent(out, depth + 1).append(quote(child.getLocalName())).append(": ");
            writeValue(out, child, depth + 1);
            out.append(i < children.size() - 1 ? ",\n" : "\n");
        }
        indent(out, depth).append('}');
    }

    private static StringBuilder indent(StringBuilder out, int depth) {
        return out.append("  ".repeat(depth));
    }

    private static String quote(String s) {
        StringBuilder q = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> q.append("\\\"");
                case '\\' -> q.append("\\\\");
                case '\n' -> q.append("\\n");
                case '\r' -> q.append("\\r");
                case '\t' -> q.append("\\t");
                default -> {
                    if (c < 0x20) {
                        q.append(String.format("\\u%04x", (int) c));
                    } else {
                        q.append(c);
                    }
                }
            }
        }
        return q.append('"').toString();
    }
}
