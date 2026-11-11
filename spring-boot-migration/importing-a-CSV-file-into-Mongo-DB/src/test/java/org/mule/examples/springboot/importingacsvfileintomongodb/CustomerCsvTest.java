package org.mule.examples.springboot.importingacsvfileintomongodb;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerCsvTest {

    @Test
    void missingColumnsBecomeNullAndExtraColumnsAreDropped() {
        var rows = CustomerCsv.parse("surname,firstname,city\nDoe,John,Austin\n");
        assertThat(rows).containsExactly(new java.util.LinkedHashMap<>(Map.of("firstname", "John", "surname", "Doe")) {{
            put("phone", null);
            put("email", null);
        }});
        assertThat(rows.get(0).keySet()).containsExactly("firstname", "surname", "phone", "email");
    }
}
