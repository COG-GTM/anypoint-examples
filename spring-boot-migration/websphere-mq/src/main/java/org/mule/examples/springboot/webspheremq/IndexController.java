package org.mule.examples.springboot.webspheremq;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** {@code ajax:connector resourceBase="${app.home}/docroot"}: the example page at /services/wmqExample. */
@Controller
public class IndexController {

    @GetMapping({"/services/wmqExample", "/services/wmqExample/"})
    public String index() {
        return "forward:/services/wmqExample/index.html";
    }
}
