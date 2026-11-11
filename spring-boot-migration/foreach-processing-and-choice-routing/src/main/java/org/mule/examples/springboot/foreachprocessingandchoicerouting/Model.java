package org.mule.examples.springboot.foreachprocessingandchoicerouting;

/** Ports of the loanbroker model classes (Customer, CustomerQuoteRequest, CreditProfile, LoanQuote). */
public final class Model {

    private Model() {
    }

    public record Customer(String name, int ssn) {
    }

    public record CustomerQuoteRequest(Customer customer, int loanAmount, int loanDuration) {
    }

    public record CreditProfile(int creditScore, int creditHistory) {
        @Override
        public String toString() {
            return "creditScore: " + creditScore + ", creditHistory: " + creditHistory;
        }
    }

    public record LoanBrokerQuoteRequest(CreditProfile creditProfile) {
    }

    public record LoanQuote(String bankName, double interestRate) {
        /** {@code LoanQuote.toString()}, which object-to-string-transformer turns into the HTTP response. */
        @Override
        public String toString() {
            return bankName + ", rate: " + interestRate;
        }
    }
}
