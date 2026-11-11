package org.mule.examples.springboot.loginformusingthehttpconnector;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class LoginController {

    static final String VALID_USERNAME = "mule";
    static final String VALID_PASSWORD = "mule";

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final MediaType TEXT_HTML_UTF8 = MediaType.parseMediaType("text/html; charset=UTF-8");

    private final LoginTemplates templates;
    private final Environment environment;
    private final RestClient restClient;

    public LoginController(LoginTemplates templates, Environment environment, RestClient.Builder restClientBuilder) {
        this.templates = templates;
        this.environment = environment;
        this.restClient = restClientBuilder.build();
    }

    /** Mule flow {@code loginPage}: GET /login → parse-template login/index.html. */
    @GetMapping(path = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String loginPage() {
        return templates.page();
    }

    /**
     * Mule flow {@code doLogin}: logger, message-filter (username == 'mule' && password == 'mule'), success template
     * with Content-Type text/html; charset=UTF-8, or catch-exception-strategy → 403 + failure template.
     */
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> doLogin(@RequestParam(required = false) String username,
                                          @RequestParam(required = false) String password) {
        log.info("User {} is trying to login", username);
        if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
            return ResponseEntity.ok().contentType(TEXT_HTML_UTF8).body(templates.success(username));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_HTML)
                .body(templates.failure(username));
    }

    /**
     * Mule flow {@code requesterLogin}: builds the form map {username=mule, password=mule} and POSTs it to this
     * application's own /login with http:request, returning that response to the caller.
     */
    @RequestMapping(path = "/requesterLogin")
    public ResponseEntity<String> requesterLogin() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", VALID_USERNAME);
        form.add("password", VALID_PASSWORD);
        ResponseEntity<String> response = restClient.post()
                .uri(loginRequestBaseUrl() + "/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toEntity(String.class);
        return ResponseEntity.status(response.getStatusCode())
                .contentType(Objects.requireNonNullElse(response.getHeaders().getContentType(), TEXT_HTML_UTF8))
                .body(response.getBody());
    }

    private String loginRequestBaseUrl() {
        String configured = environment.getProperty("login.request.base-url");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8081"));
        return "http://localhost:" + port;
    }
}
