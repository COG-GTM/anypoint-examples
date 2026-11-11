package org.mule.examples.springboot.muleexpressionlanguagebasics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Table-driven MEL → SpEL parity for the greeting.xml expressions. */
class GreetingExpressionsTableTest {

    private final GreetingExpressions spel = new GreetingExpressions();

    @ParameterizedTest(name = "greet2 username={0} -> {1}")
    @CsvSource(nullValues = "<absent>", value = {
            "Mule,      Hello Mule",
            "<absent>,  No username provided",
            "'',        No username provided",
            "'   ',     No username provided"
    })
    void choiceOnEmptyUsername(String username, String muleReply) {
        String reply = spel.usernameEmpty(username) ? "No username provided" : spel.greeting(username);
        assertThat(reply).isEqualTo(muleReply);
    }

    @ParameterizedTest(name = "summary {0},{1} -> {2}")
    @CsvSource({
            "Mule, 1,   'Mule, 1, false'",
            "Max,  18,  'Max, 18, false'",
            "Max,  19,  'Max, 19, true'",
            "Ann,  30.5,'Ann, 30.5, true'"
    })
    void summaryLine(String username, String age, String muleLine) {
        assertThat(spel.summary(username, age)).isEqualTo(muleLine);
    }

    @ParameterizedTest(name = "csv {0},{1} -> {2}")
    @CsvSource({
            "Mule, 1,  'Mule,1,false'",
            "Max,  19, 'Max,19,true'"
    })
    void dataWeaveCsvRow(String username, String age, String csv) {
        assertThat(CsvWriter.row(username, age, Boolean.toString(GreetingExpressions.isAdult(age))).trim()).isEqualTo(csv);
    }
}
