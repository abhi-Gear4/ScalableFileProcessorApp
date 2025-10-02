package com.pingAssignment.scalablefileprocessor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingAssignment.scalablefileprocessor.model.Address;
import com.pingAssignment.scalablefileprocessor.model.BadData;
import com.pingAssignment.scalablefileprocessor.model.User;
import com.pingAssignment.scalablefileprocessor.repository.AddressRepository;
import com.pingAssignment.scalablefileprocessor.repository.BadDataRepository;
import com.pingAssignment.scalablefileprocessor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;

public class FileProcessorServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private BadDataRepository badDataRepository;

    @InjectMocks
    private FileProcessorService fileProcessorService;

    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetOrCreateAddress_existingAddress() {
        Address address = new Address();
        address.setStreet("123 Main");
        address.setCity("City");
        address.setPostalCode("12345");
        address.setCountry("INDIA");

        given(addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                address.getStreet(), address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry())).willReturn(Optional.of(address));
        JsonNode addressJsonNode = mapper.valueToTree(address);

        Address found = fileProcessorService.getOrCreateAddress(addressJsonNode);

        assertThat(found).isEqualTo(address);
        verify(addressRepository).findByStreetAndCityAndStateAndPostalCodeAndCountry(
                address.getStreet(), address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry());
    }

    @Test
    public void testGetOrCreateAddress_existingAddressWithNullCityState() {
        Address address = new Address();
        address.setStreet("123 NullSt");
        address.setCity(null);
        address.setState(null);
        address.setPostalCode("62704");
        address.setCountry("USA");

        given(addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                eq(address.getStreet()),
                isNull(),
                isNull(),
                eq(address.getPostalCode()),
                eq(address.getCountry())))
                .willReturn(Optional.of(address));

        JsonNode jsonNode = mapper.valueToTree(address);
        Address found = fileProcessorService.getOrCreateAddress(jsonNode);

        assertThat(found).isNotNull();
        assertThat(found.getStreet()).isEqualTo(address.getStreet());
        assertThat(found.getCity()).isNull();
        assertThat(found.getState()).isNull();
        assertThat(found.getPostalCode()).isEqualTo(address.getPostalCode());

        verify(addressRepository).findByStreetAndCityAndStateAndPostalCodeAndCountry(
                eq(address.getStreet()), isNull(), isNull(),
                eq(address.getPostalCode()), eq(address.getCountry()));
    }

    @Test
    public void testGetOrCreateAddress_notExistingAddressWithNullCityState_savesNew() {
        Address address = new Address();
        address.setStreet("456 Null St");
        address.setCity(null);
        address.setState(null);
        address.setPostalCode("67890");
        address.setCountry("OtherCountry");

        given(addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                eq(address.getStreet()), isNull(), isNull(),
                eq(address.getPostalCode()), eq(address.getCountry())))
                .willReturn(Optional.empty());

        given(addressRepository.save(any(Address.class))).willAnswer(invocation -> {
            Address a = invocation.getArgument(0);
            a.setId(10L); // simulate DB generated ID
            return a;
        });

        JsonNode jsonNode = mapper.valueToTree(address);
        Address saved = fileProcessorService.getOrCreateAddress(jsonNode);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getStreet()).isEqualTo(address.getStreet());
        assertThat(saved.getCity()).isNull();
        assertThat(saved.getState()).isNull();
        assertThat(saved.getPostalCode()).isEqualTo(address.getPostalCode());

        verify(addressRepository).findByStreetAndCityAndStateAndPostalCodeAndCountry(
                eq(address.getStreet()), isNull(), isNull(),
                eq(address.getPostalCode()), eq(address.getCountry()));
        verify(addressRepository).save(any(Address.class));
    }



    @Test
    public void testGetOrCreateAddress_newAddress() throws IOException {
        Address address = new Address();
        address.setStreet("456 St");
        address.setCity("OtherCity");
        address.setState("OT");
        address.setPostalCode("67890");
        address.setCountry("OtherCountry");

        JsonNode node = mapper.valueToTree(address);

        given(addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(Optional.empty());

        given(addressRepository.save(any(Address.class)))
                .willAnswer(invocation -> invocation.getArgument(0)); // return same address

        Address result = fileProcessorService.getOrCreateAddress(node);

        assertThat(result.getStreet()).isEqualTo("456 St");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    public void testProcessFile_validAndInvalidRecords() throws Exception {
        // Create a temp file with mixed valid and invalid records
        Path tempFile = Files.createTempFile("test", ".jsonl");
        List<String> lines = List.of(
                "{\"first_name\":\"John\",\"middle_name\":\"A\",\"last_name\":\"Smith\",\"date_of_birth\":\"1985-04-12\",\"address\":{\"street\":\"123 Main St\",\"city\":\"City\",\"state\":\"ST\",\"postal_code\":\"12345\",\"country\":\"USA\"}}",
                "{\"first_name\":\"Invalid\",\"address\":{\"street\":\"\",\"city\":\"\",\"state\":\"\",\"postal_code\":\"\",\"country\":\"\"}}" // Invalid record
        );
        Files.write(tempFile, lines);

        given(addressRepository.findByStreetAndCityAndStateAndPostalCodeAndCountry(
                anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(Optional.empty());

        given(addressRepository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(userRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(badDataRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        fileProcessorService.processFile(tempFile);

        // Verify userRepository.saveAll called once for valid user(s)
        then(userRepository).should().saveAll(argThat(users -> {
            for (User user : users) {
                if (!"Smith".equals(user.getLastName()))
                    return false;
            }
            return true;
        }));

        // Verify badDataRepository.saveAll called once for bad records
        then(badDataRepository).should().saveAll(argThat(badRecords -> {
            for (BadData bad : badRecords) {
                if (!bad.getRawRecord().contains("Invalid"))
                    return false;
            }
            return true;
        }));
    }
}
