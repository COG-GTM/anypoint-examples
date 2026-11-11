package org.mule.examples.springboot.importcontactsintomsdynamics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One row of the input CSV (header {@code firstname,surname,phone,email}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CsvContact(String firstname, String surname, String phone, String email) {
}
