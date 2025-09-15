package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;

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
    void generatedXmlShouldContainMessageId() throws Exception {
        CIIMessage message = sampleMessage();
        OrderWriter writer = new OrderWriter();
        String xmlString = writer.writeToString(message);
        assertTrue(xmlString.contains("CrossIndustryOrder"));
        assertTrue(xmlString.contains("MSG-ORDER"));
    }
}
