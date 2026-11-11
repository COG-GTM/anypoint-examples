package org.mule.examples.springboot.importcontactsintomsdynamics;

/** Output of the Mule "CSV To Map" DataWeave: {@code firstname, lastname, businessphone, email}. */
public record ContactRecord(String firstname, String lastname, String businessphone, String email) {

    static ContactRecord from(CsvContact csv) {
        return new ContactRecord(csv.firstname(), csv.surname(), csv.phone(), csv.email());
    }
}
