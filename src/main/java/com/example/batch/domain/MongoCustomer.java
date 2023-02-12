package com.example.batch.domain;

import javax.persistence.Id;
import java.io.Serializable;

public class MongoCustomer implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String firstName;
    private String middleInitial;
    private String lastName;
    private String address;
    private String city;
    private String state;
    private String zip;

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", firstName='" + firstName + "\'" +
                ", middleInitial=" + middleInitial + "\'" +
                ", lastName=" + lastName + "\'" +
                ", address=" + address + "\'" +
                ", city=" + city + "\'" +
                ", state=" + state + "\'" +
                ", zip=" + zip + "\'" +
                "}";
    }
}
