package org.mule.examples.springboot.choiceexceptionstrategy;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/** The flow's {@code regex-filter}: Mule's RegExFilter accepts a value when the pattern is found in its string form. */
@Component
public class EmailFilter {

    private static final Pattern PATTERN = Pattern.compile(
            "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9  -]*[a-z0-9])?");

    public boolean accept(Object payload) {
        return payload != null && PATTERN.matcher(payload.toString()).find();
    }
}
