package com.cii.messaging.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryInformationTest {

    @Test
    void builderShouldSetFields() {
        Address address = Address.builder()
                .street("123 Main St")
                .city("Springfield")
                .postalCode("12345")
                .countryCode("US")
                .countryName("United States")
                .build();

        LocalDate date = LocalDate.of(2023, 5, 20);

        DeliveryInformation info = DeliveryInformation.builder()
                .deliveryDate(date)
                .deliveryLocationId("LOC1")
                .deliveryAddress(address)
                .deliveryPartyName("John Doe")
                .build();

        assertEquals(date, info.getDeliveryDate());
        assertEquals("LOC1", info.getDeliveryLocationId());
        assertEquals(address, info.getDeliveryAddress());
        assertEquals("John Doe", info.getDeliveryPartyName());
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        Address address = Address.builder()
                .street("123 Main St")
                .city("Springfield")
                .postalCode("12345")
                .countryCode("US")
                .countryName("United States")
                .build();

        DeliveryInformation info1 = DeliveryInformation.builder()
                .deliveryDate(LocalDate.of(2023, 5, 20))
                .deliveryLocationId("LOC1")
                .deliveryAddress(address)
                .deliveryPartyName("John Doe")
                .build();

        DeliveryInformation info2 = DeliveryInformation.builder()
                .deliveryDate(LocalDate.of(2023, 5, 20))
                .deliveryLocationId("LOC1")
                .deliveryAddress(address)
                .deliveryPartyName("John Doe")
                .build();

        DeliveryInformation info3 = DeliveryInformation.builder()
                .deliveryDate(LocalDate.of(2023, 5, 21))
                .deliveryLocationId("LOC2")
                .deliveryAddress(address)
                .deliveryPartyName("Jane Doe")
                .build();

        assertEquals(info1, info2);
        assertEquals(info1.hashCode(), info2.hashCode());
        assertNotEquals(info1, info3);
    }
}
