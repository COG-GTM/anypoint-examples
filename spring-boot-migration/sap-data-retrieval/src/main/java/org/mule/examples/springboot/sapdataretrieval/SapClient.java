package org.mule.examples.springboot.sapdataretrieval;

import java.util.List;

/**
 * Typed replacement for the three Mule {@code sap:outbound-endpoint type="function"} calls. Each method returns the
 * rows of the BAPI's result table; an empty list means the table came back empty.
 */
public interface SapClient {

    List<SapRow> findCustomers(SapRequests.CustomerFind request);

    List<SapRow> getMaterials(SapRequests.MaterialGetList request);

    List<SapRow> getSalesOrders(SapRequests.SalesOrderGetList request);
}
