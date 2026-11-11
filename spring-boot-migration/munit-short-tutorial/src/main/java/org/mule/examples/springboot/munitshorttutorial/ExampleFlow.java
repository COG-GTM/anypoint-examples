package org.mule.examples.springboot.munitshorttutorial;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code exampleFlow}: GET / sets the payload from {@code url_key}, calls exampleFlow2, then chooses on {@code my_variable}. */
@RestController
public class ExampleFlow {

    private final ExampleFlow2 exampleFlow2;

    public ExampleFlow(ExampleFlow2 exampleFlow2) {
        this.exampleFlow2 = exampleFlow2;
    }

    @GetMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String receive(@RequestParam(name = "url_key", required = false) String urlKey) {
        return process(new FlowEvent(urlKey)).getPayload().toString();
    }

    public FlowEvent process(FlowEvent event) {
        FlowEvent result = exampleFlow2.process(event);
        if ("var_value_1".equals(result.getFlowVar(SubFlows.MY_VARIABLE))) {
            result.setPayload("response_payload_1");
        } else {
            result.setPayload("response_payload_2");
        }
        return result;
    }
}
