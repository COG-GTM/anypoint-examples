package org.mule.examples.springboot.munitshorttutorial;

import org.springframework.stereotype.Component;

/** {@code exampleSub_Flow1} and {@code exampleSub_Flow2}. */
@Component
public class SubFlows {

    static final String MY_VARIABLE = "my_variable";

    public void exampleSubFlow1(FlowEvent event) {
        event.setFlowVar(MY_VARIABLE, "var_value_1");
    }

    public void exampleSubFlow2(FlowEvent event) {
        event.setFlowVar(MY_VARIABLE, "var_value_2");
    }
}
