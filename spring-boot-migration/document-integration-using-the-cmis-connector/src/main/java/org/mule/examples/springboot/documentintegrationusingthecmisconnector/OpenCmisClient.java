package org.mule.examples.springboot.documentintegrationusingthecmisconnector;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link CmisClient} on Apache Chemistry OpenCMIS (the library the Mule CMIS connector is built on), using the
 * AtomPub binding at {@code cmis.base-url} like the Mule {@code cmis:config}. The session is opened on first use.
 */
@Component
class OpenCmisClient implements CmisClient {

    private final Map<String, String> parameters;
    private volatile Session session;

    OpenCmisClient(@Value("${cmis.base-url}") String baseUrl,
                   @Value("${cmis.username}") String username,
                   @Value("${cmis.password}") String password,
                   @Value("${cmis.repository-id}") String repositoryId) {
        this.parameters = sessionParameters(baseUrl, username, password, repositoryId);
    }

    static Map<String, String> sessionParameters(String baseUrl, String username, String password, String repositoryId) {
        Map<String, String> p = new HashMap<>();
        p.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        p.put(SessionParameter.ATOMPUB_URL, baseUrl);
        p.put(SessionParameter.USER, username);
        p.put(SessionParameter.PASSWORD, password);
        p.put(SessionParameter.REPOSITORY_ID, repositoryId);
        return p;
    }

    @Override
    public String createDocumentByPathFromContent(String folderPath, String filename, byte[] content, String mimeType,
                                                  String objectType, String versioningState) {
        Session s = session();
        Folder folder = (Folder) s.getObjectByPath(folderPath);
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, objectType);
        properties.put(PropertyIds.NAME, filename);
        ContentStream stream = s.getObjectFactory().createContentStream(filename, content.length,
                mimeType, new ByteArrayInputStream(content));
        ObjectId id = s.createDocument(properties, folder, stream, VersioningState.fromValue(versioningState.toLowerCase()));
        return id.getId();
    }

    private Session session() {
        Session s = session;
        if (s == null) {
            synchronized (this) {
                if (session == null) {
                    session = SessionFactoryImpl.newInstance().createSession(parameters);
                }
                s = session;
            }
        }
        return s;
    }
}
