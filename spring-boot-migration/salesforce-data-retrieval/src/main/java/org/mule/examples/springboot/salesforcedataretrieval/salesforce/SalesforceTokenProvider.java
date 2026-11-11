package org.mule.examples.springboot.salesforcedataretrieval.salesforce;

/** Supplies the token {@link SalesforceClient} calls the REST API with. */
public interface SalesforceTokenProvider {

    /** Current token, logging in first if there is none. */
    SalesforceToken current();

    /** Called when the API answered 401 for {@code stale}; returns a fresh token. */
    SalesforceToken renew(SalesforceToken stale);
}
