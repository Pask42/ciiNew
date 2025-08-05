package com.cii.messaging.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    @Test
    void builderShouldSetFields() {
        Address address = Address.builder()
                .street("123 Main St")
                .city("Springfield")
                .postalCode("12345")
                .countryCode("US")
                .countryName("United States")
                .build();

        assertEquals("123 Main St", address.getStreet());
        assertEquals("Springfield", address.getCity());
        assertEquals("12345", address.getPostalCode());
        assertEquals("US", address.getCountryCode());
        assertEquals("United States", address.getCountryName());
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        Address address1 = Address.builder()
                .street("123 Main St")
                .city("Springfield")
                .postalCode("12345")
                .countryCode("US")
                .countryName("United States")
                .build();

        Address address2 = Address.builder()
                .street("123 Main St")
                .city("Springfield")
                .postalCode("12345")
                .countryCode("US")
                .countryName("United States")
                .build();

        Address address3 = Address.builder()
                .street("456 Elm St")
                .city("Shelbyville")
                .postalCode("54321")
                .countryCode("CA")
                .countryName("Canada")
                .build();

        assertEquals(address1, address2);
        assertEquals(address1.hashCode(), address2.hashCode());
        assertNotEquals(address1, address3);
    }
}
