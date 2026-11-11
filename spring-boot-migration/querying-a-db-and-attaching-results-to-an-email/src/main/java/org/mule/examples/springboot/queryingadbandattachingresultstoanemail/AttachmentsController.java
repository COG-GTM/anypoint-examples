package org.mule.examples.springboot.queryingadbandattachingresultstoanemail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flow {@code attachmentsFlow1}: {@code http:listener path="/"} -> {@code splitter #[xpath('//employee')]} ->
 * {@code db:select} per employee -> {@code collection-aggregator} -> logger -> DataWeave "Map Collection to CSV" ->
 * {@code object-to-string mimeType="text/csv"} -> attachment + SMTP. The listener answers with the CSV.
 */
@RestController
class AttachmentsController {

    private static final Logger log = LoggerFactory.getLogger(AttachmentsController.class);
    static final MediaType TEXT_CSV_UTF8 = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final EmployeeRepository employees;
    private final EmployeeReportMailer mailer;

    AttachmentsController(EmployeeRepository employees, EmployeeReportMailer mailer) {
        this.employees = employees;
        this.mailer = mailer;
    }

    @RequestMapping("/")
    ResponseEntity<String> queryAndEmail(@RequestBody String xml) throws Exception {
        List<List<Map<String, Object>>> results = new ArrayList<>();
        for (String employee : employeeNames(xml)) {
            // #[payload.getText().split(" ")[0]] / [1]; bound as parameters instead of concatenated into the SQL.
            String[] name = employee.split(" ");
            results.add(employees.findByName(name[0], name[1]));
        }
        log.info("{}", results);
        String csv = EmployeesCsv.write(results);
        mailer.send(csv);
        return ResponseEntity.ok().contentType(TEXT_CSV_UTF8).body(csv);
    }

    private static List<String> employeeNames(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath()
                .evaluate("//employee", document, XPathConstants.NODESET);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(nodes.item(i).getTextContent());
        }
        return names;
    }
}
