package org.mule.examples.springboot.authenticatingsalesforceusingoauth2;

import org.mule.examples.springboot.authenticatingsalesforceusingoauth2.salesforce.SalesforceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Flow {@code salesforce-oauthFlow1}: {@code http:listener path="/"} → {@code sfdc:authorize} → {@code sfdc:query}
 * → {@code foreach}/{@code logger} → {@code set-payload}. When Salesforce is not authorized yet the browser is
 * redirected to Salesforce; the OAuth callback finishes the authorization and runs the rest of the flow.
 */
@RestController
class SalesforceOAuthController {

    static final String QUERY = "SELECT id,lastname,lastmodifieddate from contact limit 10";
    private static final Logger log = LoggerFactory.getLogger(SalesforceOAuthController.class);

    private final SalesforceOAuth oauth;
    private final SalesforceClient salesforce;

    SalesforceOAuthController(SalesforceOAuth oauth, SalesforceClient salesforce) {
        this.oauth = oauth;
        this.salesforce = salesforce;
    }

    @GetMapping("/")
    ResponseEntity<String> root() {
        if (!oauth.authorized()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(oauth.authorizationRedirect())).build();
        }
        return ResponseEntity.ok(queryContacts());
    }

    @GetMapping("/${sfdc.oauth.callback.path}")
    ResponseEntity<String> callback(@RequestParam(required = false) String code,
                                    @RequestParam(required = false) String state,
                                    @RequestParam(required = false) String error,
                                    @RequestParam(name = "error_description", required = false) String errorDescription) {
        if (!oauth.isPendingState(state)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Salesforce authorization failed: unknown OAuth state");
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Salesforce authorization failed: " + (error == null ? "no authorization code" : error + " " + errorDescription));
        }
        oauth.exchange(code, state);
        return ResponseEntity.ok(queryContacts());
    }

    private String queryContacts() {
        List<Map<String, Object>> contacts = salesforce.query(QUERY);
        for (Map<String, Object> contact : contacts) {
            log.info("contact: {}", contact);
        }
        return "Salesforce query returned " + contacts.size() + " contacts.";
    }
}
