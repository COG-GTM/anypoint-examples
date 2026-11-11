package org.mule.examples.springboot.documentintegrationusingthecmisconnector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Flow {@code CMIS}: {@code http:listener path="cmis"} -> Groovy "Load file" (mulesoft-logo.jpg) ->
 * {@code cmis:create-document-by-path-from-content filename="pic#[System.currentTimeMillis()].jpg"
 * folderPath="/okm:root/" mimeType="image/jpg" objectType="cmis:document" versioningState="NONE"} ->
 * {@code json:object-to-json-transformer} (the returned {@code ObjectId} serialises as {@code {"id": ...}}).
 */
@RestController
class CmisController {

    private final CmisClient cmis;
    private final Resource document;
    private final String folderPath;

    CmisController(CmisClient cmis,
                   @Value("${cmis.document}") Resource document,
                   @Value("${cmis.folder-path}") String folderPath) {
        this.cmis = cmis;
        this.document = document;
        this.folderPath = folderPath;
    }

    @RequestMapping(path = "/cmis", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, String> createDocument() throws IOException {
        byte[] content;
        try (InputStream in = document.getInputStream()) {
            content = in.readAllBytes();
        }
        String id = cmis.createDocumentByPathFromContent(folderPath, "pic" + System.currentTimeMillis() + ".jpg",
                content, "image/jpg", "cmis:document", "NONE");
        return Map.of("id", id);
    }
}
