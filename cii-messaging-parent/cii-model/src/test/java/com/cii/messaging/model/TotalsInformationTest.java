package com.cii.messaging.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TotalsInformationTest {

    @Test
    void builderShouldSetFields() {
        TotalsInformation totals = TotalsInformation.builder()
                .lineTotalAmount(new BigDecimal("100"))
                .taxBasisAmount(new BigDecimal("100"))
                .taxTotalAmount(new BigDecimal("20"))
                .grandTotalAmount(new BigDecimal("120"))
                .prepaidAmount(new BigDecimal("0"))
                .duePayableAmount(new BigDecimal("120"))
                .build();

        assertEquals(new BigDecimal("100"), totals.getLineTotalAmount());
        assertEquals(new BigDecimal("100"), totals.getTaxBasisAmount());
        assertEquals(new BigDecimal("20"), totals.getTaxTotalAmount());
        assertEquals(new BigDecimal("120"), totals.getGrandTotalAmount());
        assertEquals(new BigDecimal("0"), totals.getPrepaidAmount());
        assertEquals(new BigDecimal("120"), totals.getDuePayableAmount());
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        TotalsInformation totals1 = TotalsInformation.builder()
                .lineTotalAmount(new BigDecimal("100"))
                .taxBasisAmount(new BigDecimal("100"))
                .taxTotalAmount(new BigDecimal("20"))
                .grandTotalAmount(new BigDecimal("120"))
                .prepaidAmount(new BigDecimal("0"))
                .duePayableAmount(new BigDecimal("120"))
                .build();

        TotalsInformation totals2 = TotalsInformation.builder()
                .lineTotalAmount(new BigDecimal("100"))
                .taxBasisAmount(new BigDecimal("100"))
                .taxTotalAmount(new BigDecimal("20"))
                .grandTotalAmount(new BigDecimal("120"))
                .prepaidAmount(new BigDecimal("0"))
                .duePayableAmount(new BigDecimal("120"))
                .build();

        TotalsInformation totals3 = TotalsInformation.builder()
                .lineTotalAmount(new BigDecimal("200"))
                .taxBasisAmount(new BigDecimal("200"))
                .taxTotalAmount(new BigDecimal("40"))
                .grandTotalAmount(new BigDecimal("240"))
                .prepaidAmount(new BigDecimal("10"))
                .duePayableAmount(new BigDecimal("230"))
                .build();

        assertEquals(totals1, totals2);
        assertEquals(totals1.hashCode(), totals2.hashCode());
        assertNotEquals(totals1, totals3);
    }
}
