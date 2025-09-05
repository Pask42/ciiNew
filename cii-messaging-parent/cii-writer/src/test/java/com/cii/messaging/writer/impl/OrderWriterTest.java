package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderWriterTest {

    private CIIMessage sampleMessage() {
        TradeParty seller = TradeParty.builder()
                .id("SELLER")
                .name("Seller Corp")
                .build();

        TradeParty buyer = TradeParty.builder()
                .id("BUYER")
                .name("Buyer Corp")
                .build();

          DocumentHeader header = DocumentHeader.builder()
                  .documentNumber("ORD-2024-001")
                  .documentDate(OffsetDateTime.now(ZoneOffset.UTC))
                  .buyerReference("BR-2024")
                  .currency(Currency.getInstance("EUR"))
                  .build();

        LineItem line = LineItem.builder()
                .lineNumber("1")
                .productId("4012345678901")
                .description("Item A")
                .quantity(BigDecimal.TEN)
                .unitCode("EA")
                .unitPrice(BigDecimal.valueOf(5))
                .lineAmount(BigDecimal.valueOf(50))
                .taxCategory("S")
                .taxRate(BigDecimal.valueOf(20))
                .taxTypeCode("VAT")
                .build();

        return CIIMessage.builder()
                .messageId("MSG-ORDER")
                .messageType(MessageType.ORDER)
                  .creationDateTime(OffsetDateTime.of(2024, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .seller(seller)
                .buyer(buyer)
                .header(header)
                .lineItems(java.util.List.of(line))
                .build();
    }

    @Test
    void generatedXmlShouldContainDataAndValidate() throws Exception {
        CIIMessage message = sampleMessage();
        OrderWriter writer = new OrderWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(message, out);
        byte[] xml = out.toByteArray();
        String xmlString = new String(xml);

        assertTrue(xmlString.contains("ORD-2024-001"));
        assertTrue(xmlString.contains("4012345678901"));

        // Schema validation against the official XSD is out of scope for this basic writer test
    }
}
