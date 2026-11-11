package org.mule.examples.springboot.importingacsvfileintomssharepoint;

/** The two Microsoft SharePoint 2013 connector operations the Mule flow uses. */
public interface SharePointClient {

    /** {@code sharepoint:folder-create url="..."}. */
    void folderCreate(String serverRelativeUrl);

    /** {@code sharepoint:file-add fileServerRelativeUrl="..." overwrite="..."}. */
    void fileAdd(String fileServerRelativeUrl, byte[] content, boolean overwrite);
}
