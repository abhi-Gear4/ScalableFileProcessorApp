package com.pingAssignment.scalablefileprocessor.repository;

import com.pingAssignment.scalablefileprocessor.model.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class AddressRepositoryTest {
    @Mock
    private AddressRepository addressRepository;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindByStreetAndCityAndStateAndPostalCodeAndCountry() {
        Address address = new Address();
        address.setStreet("123 Main");
        address.setCity("City");
        address.setState("IL");
        address.setPostalCode("62704");
        address.setCountry("USA");

        given(addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                "123 Main", "City", "IL", "62704", "USA"))
                .willReturn(Optional.of(address));

        Optional<Address> found = addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                "123 Main", "City", "IL", "62704", "USA");

        assertThat(found).isPresent();
        assertThat(found.get().getCity()).isEqualTo("City");
    }

    @Test
    public void testFindByStreetAndCity_ThrowsNoSuchElementExceptionOnGet() {
        given(addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                "Missing", "Place", "YY", "11111", "Noland"))
                .willReturn(Optional.empty());

        Optional<Address> found = addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                "Missing", "Place", "YY", "11111", "Noland");

        assertThrows(NoSuchElementException.class, found::get);
    }

    @Test
    public void testSave_ThrowsRuntimeException() {
        Address address = new Address();
        address.setStreet("Error Street");
        address.setCity("Error City");
        address.setState("ZZ");
        address.setPostalCode("999");
        address.setCountry("errorland");

        doThrow(new RuntimeException("DB insert error")).when(addressRepository).save(address);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            addressRepository.save(address);
        });

        assertThat(thrown.getMessage()).isEqualTo("DB insert error");

        verify(addressRepository).save(address);
    }

    @Test
    public void testSave_NullAddress_ThrowsIllegalArgumentException() {
        doThrow(new IllegalArgumentException("Address cannot be null")).when(addressRepository).save(null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            addressRepository.save(null);
        });

        assertThat(thrown.getMessage()).isEqualTo("Address cannot be null");

        verify(addressRepository).save(null);
    }
}
