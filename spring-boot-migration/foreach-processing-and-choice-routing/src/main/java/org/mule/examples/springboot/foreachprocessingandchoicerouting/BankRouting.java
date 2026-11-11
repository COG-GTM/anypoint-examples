package org.mule.examples.springboot.foreachprocessingandchoicerouting;

import java.util.List;

/**
 * The {@code lookupBanks} choice. The second condition, {@code amount >= 10000 || amount <= 19999}, is true for every
 * int, so Bank 3 + Bank 4 is chosen for all amounts below 20000 and the Bank 5 branch is unreachable, exactly as in
 * Mule.
 */
public final class BankRouting {

    public static final String BANK_1 = "TheBank1";
    public static final String BANK_2 = "TheBank2";
    public static final String BANK_3 = "TheBank3";
    public static final String BANK_4 = "TheBank4";
    public static final String BANK_5 = "TheBank5";

    private BankRouting() {
    }

    public static List<String> banksFor(int loanAmount) {
        if (loanAmount >= 20000) {
            return List.of(BANK_1, BANK_2);
        }
        if (loanAmount >= 10000 || loanAmount <= 19999) {
            return List.of(BANK_3, BANK_4);
        }
        return List.of(BANK_5);
    }

    /** {@code findLowestLoanQuote}: strict {@code <}, so the first of equal quotes wins. */
    public static Model.LoanQuote lowest(List<Model.LoanQuote> quotes) {
        Model.LoanQuote lowest = null;
        for (Model.LoanQuote quote : quotes) {
            if (lowest == null || quote.interestRate() < lowest.interestRate()) {
                lowest = quote;
            }
        }
        return lowest;
    }
}
