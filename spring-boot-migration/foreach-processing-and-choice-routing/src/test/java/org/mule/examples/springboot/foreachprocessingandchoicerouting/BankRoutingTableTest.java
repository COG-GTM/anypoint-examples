package org.mule.examples.springboot.foreachprocessingandchoicerouting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Table-driven parity for the lookupBanks choice and findLowestLoanQuote. */
class BankRoutingTableTest {

    @ParameterizedTest(name = "amount={0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "1000000     | TheBank1,TheBank2",
            "20000       | TheBank1,TheBank2",
            "19999       | TheBank3,TheBank4",
            "10000       | TheBank3,TheBank4",
            "9999        | TheBank3,TheBank4",
            "0           | TheBank3,TheBank4",
            "-5          | TheBank3,TheBank4",
            "-2147483648 | TheBank3,TheBank4"
    })
    void routesLikeMuleChoice(int amount, String banks) {
        assertThat(BankRouting.banksFor(amount)).isEqualTo(Arrays.asList(banks.split(",")));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Bank #1:5.0,Bank #2:3.0        | Bank #2, rate: 3.0",
            "Bank #3:2.5,Bank #4:7.25       | Bank #3, rate: 2.5",
            "Bank #3:4.0,Bank #4:4.0        | Bank #3, rate: 4.0",
            "Bank #5:0.1                    | Bank #5, rate: 0.1"
    })
    void lowestQuoteKeepsFirstOnTie(String quotes, String expected) {
        List<Model.LoanQuote> list = Arrays.stream(quotes.split(","))
                .map(q -> q.split(":"))
                .map(q -> new Model.LoanQuote(q[0], Double.parseDouble(q[1])))
                .toList();
        assertThat(BankRouting.lowest(list).toString()).isEqualTo(expected);
    }
}
