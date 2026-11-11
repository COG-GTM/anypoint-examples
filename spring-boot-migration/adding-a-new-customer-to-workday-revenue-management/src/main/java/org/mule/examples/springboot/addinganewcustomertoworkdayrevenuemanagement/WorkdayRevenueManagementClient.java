package org.mule.examples.springboot.addinganewcustomertoworkdayrevenuemanagement;

import java.io.StringWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Typed replacement for the Mule {@code wd-connector:invoke type="Revenue_Management||Put_Customer"}: sends the
 * {@code Put_Customer_Request} as a SOAP 1.1 envelope (WS-Security UsernameToken {@code user@tenant}) to the Workday
 * Revenue_Management web service and reads the customer descriptor from the response.
 */
@Component
public class WorkdayRevenueManagementClient {

    static final String BSVC = "urn:com.workday/bsvc";
    static final String SOAP_ENV = "http://schemas.xmlsoap.org/soap/envelope/";
    static final String WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    static final String PASSWORD_TEXT =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";

    private final WebClient webClient;
    private final WorkdayProperties properties;
    private final String servicePath;

    public WorkdayRevenueManagementClient(WebClient.Builder builder, WorkdayProperties properties) {
        this.webClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.servicePath = "/ccx/service/" + properties.tenant() + "/Revenue_Management/" + properties.version();
    }

    public PutCustomerResponse putCustomer(PutCustomerRequest request) {
        String response = webClient.post()
                .uri(servicePath)
                .contentType(MediaType.TEXT_XML)
                .header("SOAPAction", "")
                .bodyValue(envelope(request))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return new PutCustomerResponse(descriptor(Xml.parse(response, true)));
    }

    String envelope(PutCustomerRequest r) {
        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter w = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("soapenv", "Envelope", SOAP_ENV);
            w.writeNamespace("soapenv", SOAP_ENV);
            w.writeStartElement("soapenv", "Header", SOAP_ENV);
            w.writeStartElement("wsse", "Security", WSSE);
            w.writeNamespace("wsse", WSSE);
            w.writeStartElement("wsse", "UsernameToken", WSSE);
            text(w, "wsse", "Username", WSSE, properties.user() + "@" + properties.tenant());
            w.writeStartElement("wsse", "Password", WSSE);
            w.writeAttribute("Type", PASSWORD_TEXT);
            w.writeCharacters(properties.password());
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeStartElement("soapenv", "Body", SOAP_ENV);
            w.writeStartElement("ns0", "Put_Customer_Request", BSVC);
            w.writeNamespace("ns0", BSVC);
            w.writeStartElement("ns0", "Customer_Data", BSVC);
            text(w, "ns0", "Customer_ID", BSVC, r.customerId());
            text(w, "ns0", "Customer_Reference_ID", BSVC, r.customerReferenceId());
            text(w, "ns0", "Customer_Name", BSVC, r.customerName());
            w.writeStartElement("ns0", "Business_Entity_Data", BSVC);
            text(w, "ns0", "Business_Entity_Name", BSVC, r.businessEntityName());
            w.writeEndElement();
            w.writeStartElement("ns0", "Customer_Category_Reference", BSVC);
            id(w, r.categoryReferenceType(), r.categoryReferenceValue());
            w.writeEndElement();
            w.writeStartElement("ns0", "Customer_Status_Data", BSVC);
            w.writeStartElement("ns0", "Customer_Status_Value_Reference", BSVC);
            id(w, r.statusReferenceType(), r.statusReferenceValue());
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndDocument();
            w.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }
        return out.toString();
    }

    private static void id(XMLStreamWriter w, String type, String value) throws XMLStreamException {
        w.writeStartElement("ns0", "ID", BSVC);
        if (type != null) {
            w.writeAttribute("ns0", BSVC, "type", type);
        }
        if (value != null) {
            w.writeCharacters(value);
        }
        w.writeEndElement();
    }

    private static void text(XMLStreamWriter w, String prefix, String name, String ns, String value)
            throws XMLStreamException {
        w.writeStartElement(prefix, name, ns);
        if (value != null) {
            w.writeCharacters(value);
        }
        w.writeEndElement();
    }

    static String descriptor(Document response) {
        NodeList refs = response.getElementsByTagNameNS(BSVC, "Customer_Reference");
        if (refs.getLength() == 0) {
            return null;
        }
        Element ref = (Element) refs.item(0);
        return ref.hasAttributeNS(BSVC, "Descriptor") ? ref.getAttributeNS(BSVC, "Descriptor") : null;
    }
}
