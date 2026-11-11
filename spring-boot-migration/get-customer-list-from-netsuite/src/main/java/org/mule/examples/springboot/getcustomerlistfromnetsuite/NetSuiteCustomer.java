package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The three columns selected by the Mule DSQL query ({@code SELECT email,firstName,lastName FROM CUSTOMER}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NetSuiteCustomer(String email, String firstName, String lastName) {
}
