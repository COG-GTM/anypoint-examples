package org.mule.examples.springboot.dataweavewithflowreflookup;

import java.util.List;
import java.util.Map;

/** Typed seam for {@code sfdc:create type="Account"} (the Mule Salesforce connector cannot run here). */
public interface SalesforceClient {

    void create(String type, List<Map<String, Object>> objects);
}
