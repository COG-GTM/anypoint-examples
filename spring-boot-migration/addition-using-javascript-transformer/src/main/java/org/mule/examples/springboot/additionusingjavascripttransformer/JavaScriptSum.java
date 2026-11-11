package org.mule.examples.springboot.additionusingjavascripttransformer;

import java.math.BigDecimal;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Java port of the Mule {@code scripting:transformer} (JavaScript/Rhino):
 * <pre>
 * var list = eval("(" + payload + ")"); var sum = 0; for (var i in list) sum += list[i]; payload = sum;
 * </pre>
 * {@code +} follows JavaScript semantics: numeric addition until a string operand turns the sum into a string
 * concatenation. A numeric result is a Rhino {@code Double}, so Mule renders it with {@link Double#toString(double)}.
 */
public final class JavaScriptSum {

    private JavaScriptSum() {
    }

    /** Returns the value Mule would print for {@code #[payload]} after the script ran. */
    public static String sum(JsonNode list) {
        Object sum = 0d;
        Iterator<JsonNode> values = list.isContainerNode() ? list.elements() : java.util.Collections.emptyIterator();
        while (values.hasNext()) {
            sum = plus(sum, values.next());
        }
        return sum instanceof Double d ? Double.toString(d) : (String) sum;
    }

    private static Object plus(Object left, JsonNode right) {
        if (left instanceof String s) {
            return s + jsString(right);
        }
        double l = (Double) left;
        if (right.isTextual() || right.isContainerNode()) {
            return jsNumberToString(l) + jsString(right);
        }
        return l + toNumber(right);
    }

    private static double toNumber(JsonNode value) {
        if (value.isNumber()) {
            return value.doubleValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue() ? 1 : 0;
        }
        return 0; // null
    }

    private static String jsString(JsonNode value) {
        if (value.isTextual()) {
            return value.textValue();
        }
        if (value.isNumber()) {
            return jsNumberToString(value.doubleValue());
        }
        if (value.isObject()) {
            return "[object Object]";
        }
        if (value.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < value.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                JsonNode e = value.get(i);
                sb.append(e.isNull() ? "" : jsString(e));
            }
            return sb.toString();
        }
        return value.asText(); // true / false / null
    }

    /** JavaScript Number#toString for the common cases: integral values print without a fraction. */
    static String jsNumberToString(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e21) {
            return new BigDecimal(d).toPlainString();
        }
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }
}
