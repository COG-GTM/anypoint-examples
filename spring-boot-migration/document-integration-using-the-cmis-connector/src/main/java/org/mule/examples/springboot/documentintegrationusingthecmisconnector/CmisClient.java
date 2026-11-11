package org.mule.examples.springboot.documentintegrationusingthecmisconnector;

/** The Mule CMIS connector operation the flow uses, as a typed client. */
public interface CmisClient {

    /**
     * {@code cmis:create-document-by-path-from-content}: creates a document in the folder at {@code folderPath} and
     * returns the new object's id (the connector's {@code ObjectId}).
     */
    String createDocumentByPathFromContent(String folderPath, String filename, byte[] content, String mimeType,
                                           String objectType, String versioningState);
}
