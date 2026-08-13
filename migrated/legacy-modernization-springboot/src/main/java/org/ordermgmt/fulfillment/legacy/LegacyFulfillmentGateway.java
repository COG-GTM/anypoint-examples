package org.ordermgmt.fulfillment.legacy;

/**
 * Outbound port to the legacy, file-based fulfillment system. The Mule application used a
 * {@code file:outbound-endpoint}; keeping this behind an interface lets a real deployment swap in
 * SFTP, JMS or an HTTP call without touching the flow logic.
 */
public interface LegacyFulfillmentGateway {

    void send(String csvPayload);
}
