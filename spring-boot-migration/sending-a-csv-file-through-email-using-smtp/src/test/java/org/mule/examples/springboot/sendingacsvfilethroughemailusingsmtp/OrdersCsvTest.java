package org.mule.examples.springboot.sendingacsvfilethroughemailusingsmtp;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrdersCsvTest {

    @Test
    void integralNumbersBecomeIntegers() {
        assertThat(OrdersCsv.javaNumber(new BigDecimal("2.0"))).isEqualTo(2);
        assertThat(OrdersCsv.javaNumber(new BigDecimal("20.750"))).hasToString("20.75");
        assertThat(OrdersCsv.javaNumber(new BigDecimal("1E+1"))).isEqualTo(10);
    }
}
