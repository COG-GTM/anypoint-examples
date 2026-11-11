package org.mule.examples.springboot.additionusingjavascripttransformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Table-driven transformer parity: each row is what the Mule JavaScript (Rhino) transformer followed by
 * {@code set-payload "Sum is: #[payload]."} produces for one JSON payload.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class JavaScriptSumTableTest {

    @Autowired
    CalculatorGateway gateway;

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "{\"a\":1,\"b\":2}                  | Sum is: 3.0.",
            "{\"a\":3,\"b\":4}                  | Sum is: 7.0.",
            "{}                                 | Sum is: 0.0.",
            "{\"x\":0.1,\"y\":0.2}              | Sum is: 0.30000000000000004.",
            "{\"a\":-5,\"b\":2}                 | Sum is: -3.0.",
            "{\"a\":true,\"b\":2}               | Sum is: 3.0.",
            "{\"a\":null,\"b\":2}               | Sum is: 2.0.",
            "{\"a\":1,\"b\":\"2\",\"c\":3}      | Sum is: 123.",
            "{\"a\":\"x\"}                      | Sum is: 0x.",
            "[1,2,3]                            | Sum is: 6.0."
    })
    void sumsLikeRhino(String payload, String muleResponse) {
        assertThat(gateway.calculate(payload)).isEqualTo(muleResponse);
    }
}
