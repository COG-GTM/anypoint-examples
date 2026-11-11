package org.mule.examples.springboot.choiceexceptionstrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

/**
 * Renders a parsed JSON object the way the Mule example's {@code #[payload]} printed its {@code java.util.HashMap}
 * in the documented output (README), i.e. in JDK 7 {@code HashMap} iteration order. JDK 8+ changed the hash spreading
 * and collision handling, so a plain {@code HashMap.toString()} on Java 17 orders the keys differently.
 */
@Component
public class MulePayloadRenderer {

    private static final float LOAD_FACTOR = 0.75f;

    public String render(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringJoiner joiner = new StringJoiner(", ", "{", "}");
            for (Map.Entry<?, ?> e : jdk7Order(map)) {
                joiner.add(render(e.getKey()) + "=" + render(e.getValue()));
            }
            return joiner.toString();
        }
        if (value instanceof Collection<?> c) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            c.forEach(v -> joiner.add(render(v)));
            return joiner.toString();
        }
        return String.valueOf(value);
    }

    /** Replays the insertions of {@code map} (in its own iteration order) into a simulated JDK 7 HashMap. */
    static List<Map.Entry<?, ?>> jdk7Order(Map<?, ?> map) {
        List<List<Map.Entry<?, ?>>> table = newTable(16);
        int threshold = (int) (16 * LOAD_FACTOR);
        int size = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            int hash = hash(entry.getKey());
            if (size >= threshold && !table.get(hash & (table.size() - 1)).isEmpty()) {
                List<List<Map.Entry<?, ?>>> resized = newTable(table.size() * 2);
                for (List<Map.Entry<?, ?>> bucket : table) {
                    for (Map.Entry<?, ?> e : bucket) {
                        resized.get(hash(e.getKey()) & (resized.size() - 1)).add(0, e);
                    }
                }
                table = resized;
                threshold = (int) (table.size() * LOAD_FACTOR);
            }
            table.get(hash & (table.size() - 1)).add(0, entry);
            size++;
        }
        List<Map.Entry<?, ?>> ordered = new ArrayList<>(size);
        table.forEach(ordered::addAll);
        return ordered;
    }

    private static List<List<Map.Entry<?, ?>>> newTable(int capacity) {
        List<List<Map.Entry<?, ?>>> table = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            table.add(new ArrayList<>());
        }
        return table;
    }

    private static int hash(Object key) {
        if (key == null) {
            return 0;
        }
        int h = key.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }
}
