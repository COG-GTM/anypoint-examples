package com.example.importleads.model;

import com.opencsv.bean.CsvBindByName;

public class Lead {

    @CsvBindByName(column = "Company")
    private String company;

    @CsvBindByName(column = "Email")
    private String email;

    @CsvBindByName(column = "FirstName")
    private String firstName;

    @CsvBindByName(column = "LastName")
    private String lastName;

    @CsvBindByName(column = "Birthday")
    private String birthday;

    private String salesforceId;

    public Lead() {
    }

    public Lead(String company, String email, String firstName, String lastName) {
        this.company = company;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }

    @Override
    public String toString() {
        return "Lead{" +
                "company='" + company + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
