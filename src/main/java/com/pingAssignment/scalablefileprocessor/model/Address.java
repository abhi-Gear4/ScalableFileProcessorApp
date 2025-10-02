package com.pingAssignment.scalablefileprocessor.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "address",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "street", "city", "state", "postalCode", "country"
        })
)
@Data
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String state;
    @Column(name = "postal_code")
    private String postalCode;
    private String country;

}
