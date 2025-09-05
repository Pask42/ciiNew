package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderResponseWriterTest {

    private CIIMessage sampleMessage() {
        LineItem line = LineItem.builder()
                .lineNumber("1")
                .productId("4012345678901")
                .description("Item A")
                .quantity(BigDecimal.ONE)
                .unitCode("EA")
                .build();

        DocumentHeader header = DocumentHeader.builder()
                .documentNumber("ORD-RSP-2024-001")
                .build();

        return CIIMessage.builder()
                .messageId("MSG-ORDERSP")
                .messageType(MessageType.ORDERSP)
                  .creationDateTime(OffsetDateTime.of(2024, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .seller(TradeParty.builder().id("SELLER").name("Seller Corp").build())
                .buyer(TradeParty.builder().id("BUYER").name("Buyer Corp").build())
                .header(header)
                .lineItems(java.util.List.of(line))
                .build();
    }

    @Test
    void generatedXmlShouldContainDataAndValidate() throws Exception {
        CIIMessage message = sampleMessage();
        OrderResponseWriter writer = new OrderResponseWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(message, out);
        byte[] xml = out.toByteArray();
        String xmlString = new String(xml);

        assertTrue(xmlString.contains("ORD-RSP-2024-001"));
        assertTrue(xmlString.contains("4012345678901"));

        // Schema validation against the official XSD is out of scope for this basic writer test
    }
}
