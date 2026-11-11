package org.mule.examples.springboot.importinganemailattachmentusingthepop3connector;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * The "Map CSV to XML" DataWeave script:
 * {@code orders: { (payload map order: { orderId: $.orderId, name: $.name, units: $.units, pricePerUnit: $.pricePerUnit }) }}.
 * CSV values stay text; a missing column is written as an empty element.
 */
final class OrdersCsvToXml {

    static final List<String> FIELDS = List.of("orderId", "name", "units", "pricePerUnit");
    private static final XMLOutputFactory XML = XMLOutputFactory.newFactory();

    private OrdersCsvToXml() {
    }

    static String transform(String csv) {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        StringWriter out = new StringWriter();
        out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            XMLStreamWriter xml = XML.createXMLStreamWriter(out);
            xml.writeStartElement("orders");
            boolean any = false;
            for (CSVRecord record : parser) {
                any = true;
                xml.writeCharacters("\n  ");
                xml.writeStartElement("order");
                for (String field : FIELDS) {
                    xml.writeCharacters("\n    ");
                    if (record.isMapped(field) && record.isSet(field)) {
                        xml.writeStartElement(field);
                        xml.writeCharacters(record.get(field));
                        xml.writeEndElement();
                    } else {
                        xml.writeEmptyElement(field);
                    }
                }
                xml.writeCharacters("\n  ");
                xml.writeEndElement();
            }
            if (any) {
                xml.writeCharacters("\n");
            }
            xml.writeEndElement();
            xml.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Could not write orders XML", e);
        }
        return out.toString();
    }
}
