package com.pingAssignment.scalablefileprocessor.repository;

import com.pingAssignment.scalablefileprocessor.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    @Query("SELECT a FROM Address a WHERE a.street = :street AND a.city = :city " +
            "AND (:state IS NULL AND a.state IS NULL OR a.state = :state) " +
            "AND a.postalCode = :postalCode " +
            "AND (:country IS NULL AND a.country IS NULL OR a.country = :country)")
    Optional<Address> findByStreetAndCityAndStateAndPostalCodeAndCountry(
            String street, String city, String state, String postalCode, String country
    );
}