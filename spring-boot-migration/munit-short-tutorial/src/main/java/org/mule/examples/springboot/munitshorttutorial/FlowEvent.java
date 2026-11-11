package org.mule.examples.springboot.munitshorttutorial;

import java.util.HashMap;
import java.util.Map;

/** Minimal stand-in for a Mule event: a payload plus flow variables. */
public class FlowEvent {

    private Object payload;
    private final Map<String, Object> flowVars = new HashMap<>();

    public FlowEvent(Object payload) {
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Object getFlowVar(String name) {
        return flowVars.get(name);
    }

    public void setFlowVar(String name, Object value) {
        flowVars.put(name, value);
    }
}
