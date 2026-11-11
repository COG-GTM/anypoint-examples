package org.mule.examples.springboot.addinganewcustomertoworkdayrevenuemanagement;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Mule flow {@code add-customer-flow}: {@code http:listener path="/" allowedMethods="POST"}. */
@RestController
public class AddCustomerController {

    private final WorkdayRevenueManagementClient workday;

    public AddCustomerController(WorkdayRevenueManagementClient workday) {
        this.workday = workday;
    }

    @PostMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String addCustomer(@RequestBody String body) {
        PutCustomerResponse response = workday.putCustomer(toPutCustomerRequest(Xml.parse(body, false)));
        return "Added customer: " + response.customerDescriptor();
    }

    /** DataWeave "XML to Workday PutCustomerRequest XML" ({@code payload.root.Account.*}). */
    static PutCustomerRequest toPutCustomerRequest(Document input) {
        Element account = child(input.getDocumentElement(), "Account");
        String customerName = text(account, "CustomerName");
        return new PutCustomerRequest(
                customerName,
                customerName,
                customerName,
                text(account, "BusinessEntityName"),
                text(account, "Customer_Category_Reference_Type"),
                text(account, "Customer_Category_Reference_Value"),
                text(account, "Customer_Status_Reference_Type"),
                text(account, "Customer_Status_Reference_Value"));
    }

    private static Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element e && name.equals(e.getTagName())) {
                return e;
            }
        }
        return null;
    }

    private static String text(Element parent, String name) {
        Element e = child(parent, name);
        return e == null ? null : e.getTextContent();
    }
}
