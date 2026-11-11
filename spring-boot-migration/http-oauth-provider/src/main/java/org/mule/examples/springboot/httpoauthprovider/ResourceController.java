package org.mule.examples.springboot.httpoauthprovider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
class ResourceController {

    private final String listenerBaseUrl;
    private final String providerBaseUrl;
    private final String clientId;
    private final String clientSecret;

    ResourceController(@Value("${listener.base-url}") String listenerBaseUrl,
                       @Value("${provider.base-url}") String providerBaseUrl,
                       @Value("${provider.client.id}") String clientId,
                       @Value("${provider.client.secret}") String clientSecret) {
        this.listenerBaseUrl = listenerBaseUrl;
        this.providerBaseUrl = providerBaseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Flow protectedAuthcodeFlow: the bearer token is validated for READ_RESOURCE by the resource
     * server filter chain ({@code oauth2-provider:validate}), then set-payload + object-to-json.
     */
    @RequestMapping("/resources")
    Map<String, String> resources() {
        Map<String, String> payroll = new LinkedHashMap<>();
        payroll.put("name", "payroll");
        payroll.put("uri", listenerBaseUrl + "/resources/payroll");
        return payroll;
    }

    /** Flow redirectFlow (demo only): 302 to the token endpoint with the code it received. */
    @RequestMapping("/redirect")
    ResponseEntity<Void> redirect(@RequestParam(name = "code", required = false) String code) {
        String location = providerBaseUrl + "/token?grant_type=authorization_code&&client_id=" + clientId
                + "&client_secret=" + clientSecret + "&code=" + code + "&redirect_uri=" + listenerBaseUrl + "/redirect";
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, location).build();
    }
}
