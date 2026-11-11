package org.mule.examples.springboot.importcontactsintomsdynamics;

/** Attributes of the Dynamics {@code contact} entity set by the Mule {@code dynamicscrm:create} operation. */
public record DynamicsContact(String firstname, String lastname, String emailaddress1, String telephone1) {

    static DynamicsContact from(ContactRecord c) {
        return new DynamicsContact(c.firstname(), c.lastname(), c.email(), c.businessphone());
    }
}
