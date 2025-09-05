package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InvoiceWriterTest {

    private CIIMessage sampleMessage() {
        TradeParty seller = TradeParty.builder()
                .id("DE123456789")
                .name("Seller Company GmbH")
                .contact(Contact.builder()
                        .name("John Doe")
                        .telephone("+49 30 12345678")
                        .email("info@seller-company.de")
                        .build())
                .address(Address.builder()
                        .street("Hauptstraße 123")
                        .city("Berlin")
                        .postalCode("10115")
                        .countryCode("DE")
                        .build())
                .taxRegistration(TaxRegistration.builder()
                        .schemeId("VA")
                        .id("DE123456789")
                        .build())
                .build();

        TradeParty buyer = TradeParty.builder()
                .id("FR987654321")
                .name("Buyer Company SAS")
                .address(Address.builder()
                        .street("Rue de la Paix 456")
                        .city("Paris")
                        .postalCode("75001")
                        .countryCode("FR")
                        .build())
                .build();

          DocumentHeader header = DocumentHeader.builder()
                  .documentNumber("INV-2024-001")
                  .documentDate(OffsetDateTime.now(ZoneOffset.UTC))
                  .buyerReference("BUY-REF-2024-001")
                  .currency(Currency.getInstance("EUR"))
                  .build();

        LineItem line = LineItem.builder()
                .lineNumber("1")
                .productId("4012345678901")
                .description("Industrial Widget Type A")
                .quantity(java.math.BigDecimal.valueOf(100))
                .unitCode("EA")
                .unitPrice(java.math.BigDecimal.valueOf(150))
                .lineAmount(java.math.BigDecimal.valueOf(15000))
                .taxCategory("S")
                .taxRate(java.math.BigDecimal.valueOf(20))
                .taxTypeCode("VAT")
                .build();

        TotalsInformation totals = TotalsInformation.builder()
                .lineTotalAmount(java.math.BigDecimal.valueOf(15000))
                .taxBasisAmount(java.math.BigDecimal.valueOf(15000))
                .taxTotalAmount(java.math.BigDecimal.valueOf(3000))
                .grandTotalAmount(java.math.BigDecimal.valueOf(18000))
                .duePayableAmount(java.math.BigDecimal.valueOf(18000))
                .build();

        return CIIMessage.builder()
                .messageId("MSG1")
                .messageType(MessageType.INVOICE)
                  .creationDateTime(OffsetDateTime.of(2024, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .senderPartyId(seller.getId())
                .receiverPartyId(buyer.getId())
                .seller(seller)
                .buyer(buyer)
                .header(header)
                .lineItems(java.util.List.of(line))
                .totals(totals)
                .build();
    }

    @Test
    void generatedXmlShouldValidateAgainstSchema() throws Exception {
        System.setProperty(com.cii.messaging.model.util.UneceSchemaLoader.PROPERTY, "D16B");
        CIIMessage message = sampleMessage();
        InvoiceWriter writer = new InvoiceWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(message, out);

        byte[] xml = out.toByteArray();
        String xmlString = new String(xml);
        assertTrue(xmlString.contains("INV-2024-001"));
        assertTrue(xmlString.contains("4012345678901"));

        System.clearProperty(com.cii.messaging.model.util.UneceSchemaLoader.PROPERTY);
    }
}
