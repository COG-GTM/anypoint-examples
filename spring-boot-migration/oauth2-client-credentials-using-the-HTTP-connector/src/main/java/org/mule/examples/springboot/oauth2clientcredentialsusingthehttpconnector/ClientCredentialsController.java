package org.mule.examples.springboot.oauth2clientcredentialsusingthehttpconnector;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
class ClientCredentialsController {

    private static final Logger log = LoggerFactory.getLogger(ClientCredentialsController.class);

    private final BoxTokenManager tokens;
    private final OutboundHttp http;
    private final String boxUrl;
    private final String fimBaseUrl;

    ClientCredentialsController(BoxTokenManager tokens, OutboundHttp http,
                                @Value("${box.base-url}") String boxBaseUrl,
                                @Value("${box.base-path}") String boxBasePath,
                                @Value("${fim.base-url}") String fimBaseUrl) {
        this.tokens = tokens;
        this.http = http;
        this.boxUrl = stripSlash(boxBaseUrl) + boxBasePath + "/";
        this.fimBaseUrl = stripSlash(fimBaseUrl);
    }

    /**
     * Flow {@code httpclientcredentialsFlow}: listener {@code /oauth} → logger {@code #[payload]} →
     * {@code http:request GET /} on the OAuth-protected Box config. A 401/403 makes Mule refresh the
     * client-credentials token and resend once.
     */
    @RequestMapping("/oauth")
    ResponseEntity<byte[]> oauth(@RequestBody(required = false) byte[] payload) {
        log.info("{}", payload == null ? "" : new String(payload, StandardCharsets.UTF_8));
        OutboundHttp.Response response = callBox();
        if (response.status() == 401 || response.status() == 403) {
            try {
                tokens.refreshAccessToken();
            } catch (RuntimeException e) {
                throw new MuleFlowException(e.getMessage(), e);
            }
            response = callBox();
        }
        return OutboundHttp.toListenerResponse(response);
    }

    private OutboundHttp.Response callBox() {
        String token = tokens.accessToken();
        return http.send(HttpMethod.GET, boxUrl, h -> h.setBearerAuth(token), null);
    }

    /**
     * Flow {@code http-client-credentialsFlow}: listener {@code /FIM/*} → {@code http:request} on
     * {@code tfim01.demos.ibm.com} with the inbound method and {@code http.request.uri} (path + query).
     * The {@code client_id} uri-param has no placeholder in that path, so it is never sent.
     */
    @RequestMapping("/FIM/**")
    ResponseEntity<byte[]> fim(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String uri = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        String contentType = request.getContentType();
        OutboundHttp.Response response = http.send(HttpMethod.valueOf(request.getMethod()), fimBaseUrl + uri,
                h -> {
                    if (contentType != null) {
                        h.set(HttpHeaders.CONTENT_TYPE, contentType);
                    }
                }, body);
        return OutboundHttp.toListenerResponse(response);
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
