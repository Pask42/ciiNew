package com.cii.messaging.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class DocumentHeaderTest {

    private PaymentTerms samplePaymentTerms() {
        return PaymentTerms.builder()
                .description("Net 30")
                .dueDate(OffsetDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC))
                .paymentMeansCode("30")
                .accountNumber("123456")
                .accountName("Main Account")
                .financialInstitution("Bank")
                .build();
    }

    private DeliveryInformation sampleDeliveryInfo() {
        Address address = Address.builder()
                .street("123 Main St")
                .city("Springfield")
                .postalCode("12345")
                .countryCode("US")
                .countryName("United States")
                .build();

        return DeliveryInformation.builder()
                .deliveryDate(OffsetDateTime.of(2023, 5, 20, 0, 0, 0, 0, ZoneOffset.UTC))
                .deliveryLocationId("LOC1")
                .deliveryAddress(address)
                .deliveryPartyName("John Doe")
                .build();
    }

    @Test
    void builderShouldSetFields() {
        PaymentTerms terms = samplePaymentTerms();
        DeliveryInformation delivery = sampleDeliveryInfo();

        DocumentHeader header = DocumentHeader.builder()
                .documentNumber("INV-1")
                .documentDate(OffsetDateTime.of(2023, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .buyerReference("BUYER")
                .sellerReference("SELLER")
                .contractReference("CONTRACT")
                .currency(Currency.getInstance("USD"))
                .paymentTerms(terms)
                .delivery(delivery)
                .build();

        assertEquals("INV-1", header.getDocumentNumber());
        assertEquals(OffsetDateTime.of(2023, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC), header.getDocumentDate());
        assertEquals("BUYER", header.getBuyerReference());
        assertEquals("SELLER", header.getSellerReference());
        assertEquals("CONTRACT", header.getContractReference());
        assertEquals(Currency.getInstance("USD"), header.getCurrency());
        assertEquals(terms, header.getPaymentTerms());
        assertEquals(delivery, header.getDelivery());
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        PaymentTerms terms = samplePaymentTerms();
        DeliveryInformation delivery = sampleDeliveryInfo();

        DocumentHeader header1 = DocumentHeader.builder()
                .documentNumber("INV-1")
                .documentDate(OffsetDateTime.of(2023, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .buyerReference("BUYER")
                .sellerReference("SELLER")
                .contractReference("CONTRACT")
                .currency(Currency.getInstance("USD"))
                .paymentTerms(terms)
                .delivery(delivery)
                .build();

        DocumentHeader header2 = DocumentHeader.builder()
                .documentNumber("INV-1")
                .documentDate(OffsetDateTime.of(2023, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .buyerReference("BUYER")
                .sellerReference("SELLER")
                .contractReference("CONTRACT")
                .currency(Currency.getInstance("USD"))
                .paymentTerms(terms)
                .delivery(delivery)
                .build();

        DocumentHeader header3 = DocumentHeader.builder()
                .documentNumber("INV-2")
                .documentDate(OffsetDateTime.of(2023, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .buyerReference("OTHER")
                .sellerReference("SELLER")
                .contractReference("CONTRACT")
                .currency(Currency.getInstance("EUR"))
                .paymentTerms(terms)
                .delivery(delivery)
                .build();

        assertEquals(header1, header2);
        assertEquals(header1.hashCode(), header2.hashCode());
        assertNotEquals(header1, header3);
    }
}
