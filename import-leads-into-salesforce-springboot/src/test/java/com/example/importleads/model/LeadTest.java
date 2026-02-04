package com.example.importleads.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeadTest {

    @Test
    void lead_defaultConstructor_createsEmptyLead() {
        Lead lead = new Lead();
        
        assertNull(lead.getCompany());
        assertNull(lead.getEmail());
        assertNull(lead.getFirstName());
        assertNull(lead.getLastName());
        assertNull(lead.getSalesforceId());
    }

    @Test
    void lead_parameterizedConstructor_setsFields() {
        Lead lead = new Lead("Acme Corp", "john@acme.com", "John", "Doe");
        
        assertEquals("Acme Corp", lead.getCompany());
        assertEquals("john@acme.com", lead.getEmail());
        assertEquals("John", lead.getFirstName());
        assertEquals("Doe", lead.getLastName());
    }

    @Test
    void lead_settersAndGetters_workCorrectly() {
        Lead lead = new Lead();
        
        lead.setCompany("Tech Inc");
        lead.setEmail("jane@tech.com");
        lead.setFirstName("Jane");
        lead.setLastName("Smith");
        lead.setBirthday("01/15/1990");
        lead.setSalesforceId("00Q123456789");
        
        assertEquals("Tech Inc", lead.getCompany());
        assertEquals("jane@tech.com", lead.getEmail());
        assertEquals("Jane", lead.getFirstName());
        assertEquals("Smith", lead.getLastName());
        assertEquals("01/15/1990", lead.getBirthday());
        assertEquals("00Q123456789", lead.getSalesforceId());
    }

    @Test
    void lead_toString_containsRelevantInfo() {
        Lead lead = new Lead("Acme Corp", "john@acme.com", "John", "Doe");
        
        String toString = lead.toString();
        
        assertTrue(toString.contains("Acme Corp"));
        assertTrue(toString.contains("john@acme.com"));
        assertTrue(toString.contains("John"));
        assertTrue(toString.contains("Doe"));
    }
}
