package org.mule.examples.springboot.importingacsvfileintomssharepoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * {@link SharePointClient} over the SharePoint REST API ({@code /_api/web}), which is what the Mule SharePoint 2013
 * connector calls under the hood.
 * <ul>
 *   <li>folder-create: {@code POST /_api/web/folders} with an {@code SP.Folder} body;</li>
 *   <li>file-add: {@code POST /_api/web/GetFolderByServerRelativeUrl(@folder)/Files/add(url=@file,overwrite=true)}
 *       with the raw file content (parameter aliases keep spaces and slashes out of the path).</li>
 * </ul>
 */
@Component
class SharePointRestClient implements SharePointClient {

    static final MediaType ODATA_VERBOSE = MediaType.parseMediaType("application/json;odata=verbose");

    private final RestClient http;
    private final String siteUrl;

    SharePointRestClient(RestClient.Builder builder,
                         @Value("${Sharepoint.SiteUrl}") String siteUrl,
                         @Value("${sharepoint.access-token:}") String accessToken) {
        this.siteUrl = siteUrl.replaceAll("/+$", "");
        this.http = builder.defaultHeaders(h -> {
            h.setAccept(java.util.List.of(ODATA_VERBOSE));
            if (!accessToken.isBlank()) {
                h.setBearerAuth(accessToken);
            }
        }).build();
    }

    @Override
    public void folderCreate(String serverRelativeUrl) {
        http.post()
                .uri(URI.create(siteUrl + "/_api/web/folders"))
                .contentType(ODATA_VERBOSE)
                .body(Map.of("__metadata", Map.of("type", "SP.Folder"), "ServerRelativeUrl", serverRelativeUrl))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void fileAdd(String fileServerRelativeUrl, byte[] content, boolean overwrite) {
        int slash = fileServerRelativeUrl.lastIndexOf('/');
        String folder = fileServerRelativeUrl.substring(0, slash);
        String fileName = fileServerRelativeUrl.substring(slash + 1);
        URI uri = UriComponentsBuilder.fromHttpUrl(siteUrl)
                .path("/_api/web/GetFolderByServerRelativeUrl(@folder)/Files/add(url=@file,overwrite=" + overwrite + ")")
                .queryParam("@folder", "'" + folder.replace("'", "''") + "'")
                .queryParam("@file", "'" + fileName.replace("'", "''") + "'")
                .encode()
                .build()
                .toUri();
        http.post()
                .uri(uri)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(content)
                .retrieve()
                .toBodilessEntity();
    }
}
