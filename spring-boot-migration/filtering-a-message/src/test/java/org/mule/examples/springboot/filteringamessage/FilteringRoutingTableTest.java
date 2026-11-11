package org.mule.examples.springboot.filteringamessage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Table-driven filter parity: each row is the outcome of the Mule {@code custom-filter}
 * (FreeMembershipDiscountFilter) for one message; the same message is fed through the Spring Integration filter.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FilteringRoutingTableTest {

    @Autowired
    DiscountGateway gateway;

    @ParameterizedTest(name = "{0} member, {1} months, {2} purchases -> passes={3}")
    @CsvSource({
            // membership, months, purchases, passes Mule filter
            "free,     12, 2000, true",
            "free,      6,  100, false",
            "free,      0, 2500, true",
            "free,      6, 2499, false",
            "free,      7, 2000, true",
            "free,     11, 1999, false",
            "free,     12, 1500, true",
            "free,     24, 1499, false",
            "premium,   6, 1000, true",
            "premium,   5, 9999, false",
            "premium,  60,  999, false",
            "Premium,   6, 1500, false",
            "gold,      6, 2500, true"
    })
    void filtersLikeMule(String membership, int months, int purchases, boolean passes) {
        Map<String, Object> message = Map.of("membership", membership, "months", months, "purchases", purchases);

        assertThat(FreeMembershipDiscountFilter.accept(message)).isEqualTo(passes);
        assertThat(gateway.evaluate(message)).isEqualTo(passes ? "the discount was granted." : "");
    }
}
