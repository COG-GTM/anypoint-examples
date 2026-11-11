package org.mule.examples.springboot.muleexpressionlanguagebasics;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** The six {@code greetingFlow*} flows of greeting.xml (listeners accept any HTTP method, like Mule). */
@RestController
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
public class GreetingController {

    private final GreetingExpressions mel;
    private final OutputFileWriter files;
    private final ObjectMapper mapper;

    public GreetingController(GreetingExpressions mel, OutputFileWriter files, ObjectMapper mapper) {
        this.mel = mel;
        this.files = files;
        this.mapper = mapper;
    }

    /** greetingFlow1: set-payload "Hello #[username]". */
    @RequestMapping("/greet1")
    public String greet1(@RequestParam Map<String, String> query) {
        return mel.greeting(query.get("username"));
    }

    /** greetingFlow2: choice on {@code username == empty}. */
    @RequestMapping("/greet2")
    public String greet2(@RequestParam Map<String, String> query) {
        String username = query.get("username");
        return mel.usernameEmpty(username) ? "No username provided" : mel.greeting(username);
    }

    /** docs-greetingFlow3: write "username, age, age > 18" to a file, reply with the greeting. */
    @RequestMapping("/greet3")
    public String greet3(@RequestParam Map<String, String> query) {
        String username = query.get("username");
        files.write(mel.summary(username, query.get("age")));
        return mel.greeting(username);
    }

    /** docs-greetingFlow4: map → DataWeave CSV (header=false) → file, reply with the greeting. */
    @RequestMapping("/greet4")
    public String greet4(@RequestParam Map<String, String> query) {
        String username = query.get("username");
        String age = query.get("age");
        files.write(CsvWriter.row(username, age, Boolean.toString(GreetingExpressions.isAdult(age))));
        return mel.greeting(username);
    }

    /** docs-greetingFlow5: XPath {@code /user/username} and {@code /user/age}. */
    @RequestMapping("/greet5")
    public String greet5(@RequestBody byte[] body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body));
        var xpath = XPathFactory.newInstance().newXPath();
        String username = xpath.evaluate("/user/username", doc);
        String age = xpath.evaluate("/user/age", doc);
        files.write(mel.summary(username, age));
        return mel.greeting(username);
    }

    /** greetingFlow6: JSON → object, {@code payload.username} / {@code payload.age}. */
    @RequestMapping("/greet6")
    public String greet6(@RequestBody byte[] body) throws Exception {
        JsonNode json = mapper.readTree(body);
        Object username = value(json.get("username"));
        Object age = value(json.get("age"));
        files.write(mel.summary(username, age));
        return mel.greeting(username == null ? null : username.toString());
    }

    private static Object value(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isNumber() ? node.numberValue() : node.asText();
    }
}
