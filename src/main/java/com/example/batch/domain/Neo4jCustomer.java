package com.example.batch.domain;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.id.UuidStrategy;

import java.io.Serializable;
import java.util.UUID;

@NodeEntity
public class Neo4jCustomer implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    private UUID id;
    private String firstName;
    private String middleInitial;
    private String lastName;
    private String address;
    private String city;
    private String state;
    private String zip;

    @Override
    public String toString() {
        return "Neo4jCustomer {" +
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
}
