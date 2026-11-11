package org.mule.examples.springboot.salesforcedataretrieval;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The string forms Mule 3.8 produced in this flow, reproduced from {@code mule-core-3.8.0}:
 * {@code byte-array-to-string-transformer} is {@code ObjectToString}, which renders a {@code Map} with
 * {@code MapUtils.toString(map, false)} ({@code {k=v, k=v}}, values of {@code password} keys masked as
 * {@code *****}) and a {@code Collection} with {@code CollectionUtils.toString(c, 50)} ({@code [a, b]},
 * cut after 50 elements with a trailing {@code " [..]"}).
 */
final class MuleText {

    static final int COLLECTION_LIMIT = 50;
    private static final List<String> MASKED_KEYS = List.of("password");

    private MuleText() {
    }

    static String map(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            sb.append(e.getKey()).append('=')
                    .append(MASKED_KEYS.contains(e.getKey()) ? "*****" : String.valueOf(e.getValue()));
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.append('}').toString();
    }

    static String collection(Collection<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        int shown = Math.min(items.size(), COLLECTION_LIMIT);
        StringBuilder sb = new StringBuilder("[");
        Iterator<String> it = items.iterator();
        for (int i = 0; i < shown; i++) {
            sb.append(it.next());
            if (i < shown - 1) {
                sb.append(", ");
            }
        }
        if (items.size() > COLLECTION_LIMIT) {
            sb.append(" [..]");
        }
        return sb.append(']').toString();
    }
}
