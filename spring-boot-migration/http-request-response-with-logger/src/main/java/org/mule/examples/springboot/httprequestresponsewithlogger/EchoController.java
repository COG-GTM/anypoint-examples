package org.mule.examples.springboot.httprequestresponsewithlogger;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mule {@code EchoFlow}: listener on {@code /*} → set-payload {@code http.request.path} → logger. */
@RestController
public class EchoController {

    private static final Logger log = LoggerFactory.getLogger(EchoController.class);

    @RequestMapping("/**")
    public String echo(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        log.info("About to echo {}", path);
        return path;
    }
}
