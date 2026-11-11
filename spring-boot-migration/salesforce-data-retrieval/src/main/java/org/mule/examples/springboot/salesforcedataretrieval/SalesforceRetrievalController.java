package org.mule.examples.springboot.salesforcedataretrieval;

import org.mule.examples.springboot.salesforcedataretrieval.salesforce.SalesforceClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Flows {@code showFormFlow} (GET /) and {@code salesforceDataRetrievalFlow} (POST /). */
@RestController
class SalesforceRetrievalController {

    static final String INVALID_QUERY = "Invalid Salesforce query.";
    private static final Logger log = LoggerFactory.getLogger(SalesforceRetrievalController.class);

    private final SalesforceClient salesforce;
    private final String template;

    SalesforceRetrievalController(SalesforceClient salesforce) throws IOException {
        this.salesforce = salesforce;
        this.template = new ClassPathResource("retrieval/index.html").getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * {@code sfdc:describe-global} → DataWeave {@code div: { (payload.sobjects map { option @(value: $.name): $.label }) }}
     * as {@code application/xml} → {@code parse-template location="retrieval/index.html"}.
     */
    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    String showForm() {
        return template.replace("#[message.payload]", options(salesforce.describeGlobal()));
    }

    /** DataWeave 1.0 XML writer output (declaration, two-space indent, no trailing newline). */
    static String options(JsonNode describeGlobal) {
        List<String> options = new ArrayList<>();
        describeGlobal.path("sobjects").forEach(o -> options.add("  <option value=\"" + xml(o.path("name").asText())
                + "\">" + xml(o.path("label").asText()) + "</option>"));
        String declaration = "<?xml version='1.0' encoding='UTF-8'?>\n";
        return options.isEmpty() ? declaration + "<div/>" : declaration + "<div>\n" + String.join("\n", options) + "\n</div>";
    }

    private static String xml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * {@code sfdc:query-all} with the MEL-built SOQL, each record turned into its Mule string form and collected
     * into a list; any failure is answered by the {@code catch-exception-strategy} with "Invalid Salesforce query.".
     */
    @PostMapping(path = "/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String retrieve(@RequestParam Map<String, String> form) {
        try {
            List<String> response = new ArrayList<>();
            for (Map<String, Object> record : salesforce.queryAll(soql(form))) {
                response.add(MuleText.map(record));
            }
            String result = MuleText.collection(response);
            log.info("Result: {}", result);
            return result;
        } catch (RuntimeException e) {
            log.debug("Salesforce query failed", e);
            return INVALID_QUERY;
        }
    }

    /**
     * {@code "SELECT id, name " + (payload.field.isEmpty() ? "" : "," + payload.field) + " from " + payload.object
     * + " where " + payload.searchKey + " like '%" + payload.searchValue + "%'"}: a missing {@code field} fails
     * ({@code null.isEmpty()}), other missing values concatenate as {@code null}.
     */
    static String soql(Map<String, String> form) {
        String field = form.get("field");
        if (field == null) {
            throw new IllegalArgumentException("field is required");
        }
        return "SELECT id, name " + (field.isEmpty() ? "" : "," + field) + " from " + form.get("object")
                + " where " + form.get("searchKey") + " like '%" + form.get("searchValue") + "%'";
    }

}
