package com.example.batch.domain;

import org.springframework.data.gemfire.mapping.annotation.Region;

import java.io.Serializable;

@Region(value = "Customers")
public class GemfireCustomer implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String firstName;
    private String middleInitial;
    private String lastName;
    private String address;
    private String city;
    private String state;
    private String zip;

    @Override
    public String toString() {
        return "GemfireCustomer {" +
                "id=" + id +
                ", firstName='" + firstName + "\'" +
                ", middleInitial='" + middleInitial + "\'" +
                ", lastName='" + lastName + "\'" +
                ", address='" + address + "\'" +
                ", city='" + city + "\'" +
                ", state='" + state + "\'" +
                ", zip='" + zip + "\'" +
                "}";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleInitial() {
        return middleInitial;
    }

    public void setMiddleInitial(String middleInitial) {
        this.middleInitial = middleInitial;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }
}
