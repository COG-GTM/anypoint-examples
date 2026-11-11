package org.mule.examples.springboot.webserviceconsumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

/**
 * Mule {@code ws:consumer-config} for the T-shirt service ({@code tshirt.wsdl}, port {@code TshirtServicePort},
 * SOAP 1.1 document/literal) on Spring-WS {@link WebServiceTemplate}. Returns the SOAP Body's first child,
 * like the Mule consumer does.
 */
@Component
class TshirtServiceClient {

    static final String NS = "http://mulesoft.org/tshirt-service";
    static final String ORDER_TSHIRT_ACTION = NS + "/order-tshirt";
    static final String LIST_INVENTORY_ACTION = NS + "/list-inventory";

    private final WebServiceTemplate template;
    private final String apiKey;

    TshirtServiceClient(@Value("${tshirt.service.address}") String address,
                        @Value("${tshirt.service.api-key}") String apiKey) throws Exception {
        SaajSoapMessageFactory factory = new SaajSoapMessageFactory();
        factory.afterPropertiesSet();
        this.template = new WebServiceTemplate(factory);
        this.template.setDefaultUri(address);
        this.apiKey = apiKey;
    }

    /** Operation {@code OrderTshirt} with the {@code AuthenticationHeader} SOAP header. */
    Element orderTshirt(String orderTshirtXml) {
        String header = "<ns0:AuthenticationHeader xmlns:ns0=\"" + NS + "\"><apiKey>" + Xml.escape(apiKey) + "</apiKey></ns0:AuthenticationHeader>";
        return invoke(orderTshirtXml, ORDER_TSHIRT_ACTION, header);
    }

    /** Operation {@code ListInventory}; its input element has no content, so the request is generated. */
    Element listInventory() {
        return invoke("<ns0:ListInventory xmlns:ns0=\"" + NS + "\"/>", LIST_INVENTORY_ACTION, null);
    }

    private Element invoke(String body, String soapAction, String headerXml) {
        DOMResult result = new DOMResult();
        template.sendSourceAndReceiveToResult(new StreamSource(new StringReader(body)), message -> {
            SoapMessage soap = (SoapMessage) message;
            soap.setSoapAction(soapAction);
            SoapHeader header = soap.getSoapHeader();
            if (headerXml != null) {
                Transformer t = TransformerFactory.newInstance().newTransformer();
                t.transform(new StreamSource(new StringReader(headerXml)), header.getResult());
            } else if (soap instanceof SaajSoapMessage saaj) {
                // CXF (Mule) omits an empty soap:Header; SAAJ always creates one.
                try {
                    saaj.getSaajMessage().getSOAPHeader().detachNode();
                } catch (jakarta.xml.soap.SOAPException e) {
                    throw new IllegalStateException(e);
                }
            }
        }, result);
        Node node = result.getNode();
        return node instanceof org.w3c.dom.Document d ? d.getDocumentElement() : (Element) node.getFirstChild();
    }
}
