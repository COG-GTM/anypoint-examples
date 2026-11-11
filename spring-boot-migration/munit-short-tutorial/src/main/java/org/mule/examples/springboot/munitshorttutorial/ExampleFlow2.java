package org.mule.examples.springboot.munitshorttutorial;

import org.springframework.stereotype.Component;

/** {@code exampleFlow2}: choice on the payload that routes to one of the two sub-flows. */
@Component
public class ExampleFlow2 {

    private final SubFlows subFlows;

    public ExampleFlow2(SubFlows subFlows) {
        this.subFlows = subFlows;
    }

    public FlowEvent process(FlowEvent event) {
        if ("payload_1".equals(event.getPayload())) {
            subFlows.exampleSubFlow1(event);
        } else {
            subFlows.exampleSubFlow2(event);
        }
        return event;
    }
}
