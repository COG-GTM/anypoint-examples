package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
class WebController {

    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    private final BoxAuthorizationCodeClient oauth;
    private final RestClient rest;
    private final String loginRedirectLocation;
    private final String searchUrl;

    WebController(BoxAuthorizationCodeClient oauth, RestClient.Builder rest,
                  @Value("${web.login-redirect-location}") String loginRedirectLocation,
                  @Value("${box.api-base-url}") String apiBaseUrl) {
        this.oauth = oauth;
        this.rest = rest.build();
        this.loginRedirectLocation = loginRedirectLocation;
        this.searchUrl = (apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl)
                + "/search?query=mule";
    }

    /** Flow boxUserLoginFlow: listener {@code web}, response-builder statusCode=302 + Location header. */
    @RequestMapping("/web")
    ResponseEntity<Void> web() {
        log.info("Redirecting user to Box login page");
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, loginRedirectLocation).build();
    }

    /**
     * Flow userLoginDoneFlow: {@code http:request GET /search?query=mule} on the OAuth-protected Box
     * config (Bearer token of the default user; on 401/403 refresh and resend once), then
     * object-to-string and set-payload.
     */
    @RequestMapping("/web/loginDone")
    String loginDone() {
        Response response = search();
        if (response.status() == 401 || response.status() == 403) {
            oauth.refreshAccessToken();
            response = search();
        }
        if (response.status() >= 400) {
            throw new MuleFlowException("Response code " + response.status() + " mapped as failure.");
        }
        return "The user successfully authorized access to the resource. Searching for files containing \"mule\": "
                + response.body();
    }

    private record Response(int status, String body) {
    }

    private Response search() {
        String token = oauth.accessToken();
        return rest.get().uri(URI.create(searchUrl)).headers(h -> h.setBearerAuth(token))
                .exchange((req, res) -> new Response(res.getStatusCode().value(),
                        new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }
}
