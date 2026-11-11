package org.mule.examples.springboot.uploadtoftpafterconvertingjsontoxml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Flow {@code main}: {@code http:listener path="/"} -> DataWeave "JSON to XML" -> {@code ftp:outbound-endpoint}
 * ("Upload to FTP", {@code outputPattern="muleExample.xml"}). The listener replies with the flow's final payload,
 * i.e. the XML document that was uploaded.
 */
@RestController
class UploadToFtpController {

    static final MediaType APPLICATION_XML_UTF8 = new MediaType("application", "xml", StandardCharsets.UTF_8);

    private final ObjectMapper mapper;
    private final RemoteFileTemplate<?> uploadTemplate;

    UploadToFtpController(ObjectMapper mapper, RemoteFileTemplate<?> uploadTemplate) {
        this.mapper = mapper;
        this.uploadTemplate = uploadTemplate;
    }

    @RequestMapping("/")
    ResponseEntity<byte[]> jsonToXmlAndUpload(@RequestBody byte[] body) throws IOException {
        JsonNode json = mapper.readTree(body);
        byte[] xml = EmployeesJsonToXml.transform(json).getBytes(StandardCharsets.UTF_8);
        uploadTemplate.send(MessageBuilder.withPayload(xml).build(), FileExistsMode.REPLACE);
        return ResponseEntity.ok().contentType(APPLICATION_XML_UTF8).body(xml);
    }
}
