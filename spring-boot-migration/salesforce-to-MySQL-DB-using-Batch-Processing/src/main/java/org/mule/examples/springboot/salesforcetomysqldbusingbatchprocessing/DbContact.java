package org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing;

/**
 * DataWeave "Transform SFDC fields to DB fields" output plus the {@code recordVars['exists']} enrichment
 * of {@code queryExistingContactInDbStep}.
 */
record DbContact(String email, String firstName, String lastName, String lastModified, boolean exists) {
}
