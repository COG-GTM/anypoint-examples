package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Reproduces the Mule pipeline DataWeave ({@code div/line/tr/td} XML) -> object-to-string ->
 * parse-template {@code customer/index.html}, whose Groovy expression drops the XML declaration line.
 */
@Component
public class CustomerTableRenderer {

    static final String TEMPLATE_EXPRESSION = "#[groovy: payload.tokenize('\\n')[1..-1].join('\\n')]";

    private final String template;

    public CustomerTableRenderer() {
        try {
            this.template = new ClassPathResource("templates/customer/index.html")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String render(List<NetSuiteCustomer> customers) {
        return template.replace(TEMPLATE_EXPRESSION, rows(customers));
    }

    static String rows(List<NetSuiteCustomer> customers) {
        StringBuilder xml = new StringBuilder("<div>\n");
        if (customers.isEmpty()) {
            xml.append("  <line>\n    <tr>\n      <td>No customers found</td>\n    </tr>\n  </line>\n");
        } else {
            xml.append("  <line>\n");
            for (NetSuiteCustomer c : customers) {
                xml.append("    <tr>\n");
                td(xml, c.lastName());
                td(xml, c.firstName());
                td(xml, c.email());
                xml.append("    </tr>\n");
            }
            xml.append("  </line>\n");
        }
        return xml.append("</div>").toString();
    }

    private static void td(StringBuilder xml, String value) {
        if (value == null) {
            xml.append("      <td/>\n");
        } else {
            xml.append("      <td>").append(escape(value)).append("</td>\n");
        }
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
