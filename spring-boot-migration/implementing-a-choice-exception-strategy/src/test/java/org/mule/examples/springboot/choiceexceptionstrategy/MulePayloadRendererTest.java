package org.mule.examples.springboot.choiceexceptionstrategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class MulePayloadRendererTest {

    private final MulePayloadRenderer renderer = new MulePayloadRenderer();

    @Test
    void rendersReadmeExampleInJdk7HashMapOrder() {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("email", "aaa@aaa.aa");
        order.put("item name", "aa");
        order.put("item units", 10);
        order.put("item price per unit", -1);
        order.put("membership", "free");
        assertThat(renderer.render(order))
                .isEqualTo("{item name=aa, membership=free, item price per unit=-1, email=aaa@aaa.aa, item units=10}");
    }

    @Test
    void keepsEveryEntryAcrossResizes() {
        Map<String, Object> many = new LinkedHashMap<>();
        IntStream.range(0, 100).forEach(i -> many.put("k" + i, i));
        assertThat(MulePayloadRenderer.jdk7Order(many)).hasSize(100);
    }
}
