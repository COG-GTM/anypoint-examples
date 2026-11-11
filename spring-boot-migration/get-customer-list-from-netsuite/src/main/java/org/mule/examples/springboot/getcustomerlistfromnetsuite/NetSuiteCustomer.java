package org.mule.examples.springboot.getcustomerlistfromnetsuite;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The three columns selected by the Mule DSQL query ({@code SELECT email,firstName,lastName FROM CUSTOMER}). SuiteQL
 * returns column names in lower case, so those spellings are accepted too.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NetSuiteCustomer(String email,
                               @JsonAlias("firstname") String firstName,
                               @JsonAlias("lastname") String lastName) {
}
