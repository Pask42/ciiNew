package com.cii.messaging.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LineItemTest {

    @Test
    void builderShouldSetFields() {
        LineItem item = LineItem.builder()
                .lineNumber("1")
                .productId("P001")
                .description("Product 1")
                .quantity(new BigDecimal("2"))
                .unitCode("EA")
                .unitPrice(new BigDecimal("10"))
                .lineAmount(new BigDecimal("20"))
                .taxRate(new BigDecimal("0.2"))
                .taxCategory("VAT")
                .build();

        assertEquals("1", item.getLineNumber());
        assertEquals("P001", item.getProductId());
        assertEquals("Product 1", item.getDescription());
        assertEquals(new BigDecimal("2"), item.getQuantity());
        assertEquals("EA", item.getUnitCode());
        assertEquals(new BigDecimal("10"), item.getUnitPrice());
        assertEquals(new BigDecimal("20"), item.getLineAmount());
        assertEquals(new BigDecimal("0.2"), item.getTaxRate());
        assertEquals("VAT", item.getTaxCategory());
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        LineItem item1 = LineItem.builder()
                .lineNumber("1")
                .productId("P001")
                .description("Product 1")
                .quantity(new BigDecimal("2"))
                .unitCode("EA")
                .unitPrice(new BigDecimal("10"))
                .lineAmount(new BigDecimal("20"))
                .taxRate(new BigDecimal("0.2"))
                .taxCategory("VAT")
                .build();

        LineItem item2 = LineItem.builder()
                .lineNumber("1")
                .productId("P001")
                .description("Product 1")
                .quantity(new BigDecimal("2"))
                .unitCode("EA")
                .unitPrice(new BigDecimal("10"))
                .lineAmount(new BigDecimal("20"))
                .taxRate(new BigDecimal("0.2"))
                .taxCategory("VAT")
                .build();

        LineItem item3 = LineItem.builder()
                .lineNumber("2")
                .productId("P002")
                .description("Product 2")
                .quantity(new BigDecimal("1"))
                .unitCode("EA")
                .unitPrice(new BigDecimal("15"))
                .lineAmount(new BigDecimal("15"))
                .taxRate(new BigDecimal("0.1"))
                .taxCategory("VAT")
                .build();

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotEquals(item1, item3);
    }
}
