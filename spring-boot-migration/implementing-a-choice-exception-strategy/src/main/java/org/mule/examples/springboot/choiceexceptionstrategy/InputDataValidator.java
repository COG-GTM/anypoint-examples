package org.mule.examples.springboot.choiceexceptionstrategy;

import java.util.Map;

import org.springframework.stereotype.Component;

/** Same checks, exception types and messages as the Mule {@code org.mule.examples.InputDataValidator} custom filter. */
@Component
public class InputDataValidator {

    public void validate(Map<String, Object> payloadMap) {
        if (!payloadMap.containsKey("email"))
            throw new NullPointerException("email is missing");
        if (Integer.parseInt(payloadMap.get("item units").toString()) < 1)
            throw new IllegalArgumentException("item units is negative");
        if (Integer.parseInt(payloadMap.get("item price per unit").toString()) < 0)
            throw new IllegalArgumentException("item price per unit is negative");
    }
}
