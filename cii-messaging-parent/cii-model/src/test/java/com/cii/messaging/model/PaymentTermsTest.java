package com.cii.messaging.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTermsTest {

    @Test
    void builderShouldSetFields() {
        OffsetDateTime due = OffsetDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC);
        PaymentTerms terms = PaymentTerms.builder()
                .description("Net 30")
                .dueDate(due)
                .paymentMeansCode("30")
                .accountNumber("123456")
                .accountName("Main Account")
                .financialInstitution("Bank")
                .build();

        assertEquals("Net 30", terms.getDescription());
        assertEquals(due, terms.getDueDate());
        assertEquals("30", terms.getPaymentMeansCode());
        assertEquals("123456", terms.getAccountNumber());
        assertEquals("Main Account", terms.getAccountName());
        assertEquals("Bank", terms.getFinancialInstitution());
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        PaymentTerms terms1 = PaymentTerms.builder()
                .description("Net 30")
                .dueDate(OffsetDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC))
                .paymentMeansCode("30")
                .accountNumber("123456")
                .accountName("Main Account")
                .financialInstitution("Bank")
                .build();

        PaymentTerms terms2 = PaymentTerms.builder()
                .description("Net 30")
                .dueDate(OffsetDateTime.of(2023, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC))
                .paymentMeansCode("30")
                .accountNumber("123456")
                .accountName("Main Account")
                .financialInstitution("Bank")
                .build();

        PaymentTerms terms3 = PaymentTerms.builder()
                .description("Net 60")
                .dueDate(OffsetDateTime.of(2023, 7, 15, 0, 0, 0, 0, ZoneOffset.UTC))
                .paymentMeansCode("60")
                .accountNumber("654321")
                .accountName("Other Account")
                .financialInstitution("Other Bank")
                .build();

        assertEquals(terms1, terms2);
        assertEquals(terms1.hashCode(), terms2.hashCode());
        assertNotEquals(terms1, terms3);
    }
}
