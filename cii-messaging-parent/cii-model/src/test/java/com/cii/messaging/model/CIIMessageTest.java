package com.cii.messaging.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CIIMessageTest {

    private DocumentHeader sampleHeader() {
        PaymentTerms terms = PaymentTerms.builder()
                .description("Net 30")
                .dueDate(LocalDate.of(2023, 6, 15))
                .paymentMeansCode("30")
                .accountNumber("123456")
                .accountName("Main Account")
                .financialInstitution("Bank")
                .build();

        DeliveryInformation delivery = DeliveryInformation.builder()
                .deliveryDate(LocalDate.of(2023, 5, 20))
                .deliveryLocationId("LOC1")
                .deliveryAddress(Address.builder()
                        .street("123 Main St")
                        .city("Springfield")
                        .postalCode("12345")
                        .countryCode("US")
                        .countryName("United States")
                        .build())
                .deliveryPartyName("John Doe")
                .build();

        return DocumentHeader.builder()
                .documentNumber("INV-1")
                .documentDate(LocalDate.of(2023, 5, 1))
                .buyerReference("BUYER")
                .sellerReference("SELLER")
                .contractReference("CONTRACT")
                .currency("USD")
                .paymentTerms(terms)
                .delivery(delivery)
                .build();
    }

    private LineItem sampleLineItem() {
        return LineItem.builder()
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
    }

    private TotalsInformation sampleTotals() {
        return TotalsInformation.builder()
                .lineTotalAmount(new BigDecimal("20"))
                .taxBasisAmount(new BigDecimal("20"))
                .taxTotalAmount(new BigDecimal("4"))
                .grandTotalAmount(new BigDecimal("24"))
                .prepaidAmount(new BigDecimal("0"))
                .duePayableAmount(new BigDecimal("24"))
                .build();
    }

    private TradeParty sampleSeller() {
        return TradeParty.builder()
                .id("SENDER")
                .name("Seller")
                .address(Address.builder()
                        .street("1 Seller St")
                        .city("Sellertown")
                        .postalCode("1000")
                        .countryCode("US")
                        .build())
                .contact(Contact.builder()
                        .name("Alice Seller")
                        .telephone("+1 555 0100")
                        .email("alice@seller.test")
                        .build())
                .taxRegistration(TaxRegistration.builder()
                        .schemeId("VA")
                        .id("SENDER-TAX")
                        .build())
                .build();
    }

    private TradeParty sampleBuyer() {
        return TradeParty.builder()
                .id("RECEIVER")
                .name("Buyer")
                .address(Address.builder()
                        .street("9 Buyer Ave")
                        .city("Buyerville")
                        .postalCode("2000")
                        .countryCode("US")
                        .build())
                .build();
    }

    @Test
    void builderShouldSetFields() {
        DocumentHeader header = sampleHeader();
        LineItem lineItem = sampleLineItem();
        TotalsInformation totals = sampleTotals();
        LocalDateTime timestamp = LocalDateTime.of(2023, 5, 1, 12, 0);

        CIIMessage message = CIIMessage.builder()
                .messageId("MSG1")
                .messageType(MessageType.INVOICE)
                .creationDateTime(timestamp)
                .senderPartyId("SENDER")
                .receiverPartyId("RECEIVER")
                .seller(sampleSeller())
                .buyer(sampleBuyer())
                .header(header)
                .lineItems(List.of(lineItem))
                .totals(totals)
                .build();

        assertEquals("MSG1", message.getMessageId());
        assertEquals(MessageType.INVOICE, message.getMessageType());
        assertEquals(timestamp, message.getCreationDateTime());
        assertEquals("SENDER", message.getSenderPartyId());
        assertEquals("RECEIVER", message.getReceiverPartyId());
        assertEquals("Seller", message.getSeller().getName());
        assertEquals("Buyer", message.getBuyer().getName());
        assertEquals(header, message.getHeader());
        assertEquals(List.of(lineItem), message.getLineItems());
        assertEquals(totals, message.getTotals());
    }

    @Test
    void equalsAndHashCodeShouldWork() {
        DocumentHeader header1 = sampleHeader();
        DocumentHeader header2 = sampleHeader();
        LineItem item1 = sampleLineItem();
        LineItem item2 = sampleLineItem();
        TotalsInformation totals1 = sampleTotals();
        TotalsInformation totals2 = sampleTotals();
        LocalDateTime timestamp = LocalDateTime.of(2023, 5, 1, 12, 0);

        CIIMessage message1 = CIIMessage.builder()
                .messageId("MSG1")
                .messageType(MessageType.INVOICE)
                .creationDateTime(timestamp)
                .senderPartyId("SENDER")
                .receiverPartyId("RECEIVER")
                .seller(sampleSeller())
                .buyer(sampleBuyer())
                .header(header1)
                .lineItems(List.of(item1))
                .totals(totals1)
                .build();

        CIIMessage message2 = CIIMessage.builder()
                .messageId("MSG1")
                .messageType(MessageType.INVOICE)
                .creationDateTime(timestamp)
                .senderPartyId("SENDER")
                .receiverPartyId("RECEIVER")
                .seller(sampleSeller())
                .buyer(sampleBuyer())
                .header(header2)
                .lineItems(List.of(item2))
                .totals(totals2)
                .build();

        CIIMessage message3 = CIIMessage.builder()
                .messageId("MSG2")
                .messageType(MessageType.ORDER)
                .creationDateTime(timestamp.plusDays(1))
                .senderPartyId("OTHER")
                .receiverPartyId("RECEIVER")
                .seller(sampleSeller())
                .buyer(sampleBuyer())
                .header(header1)
                .lineItems(List.of(item1))
                .totals(totals1)
                .build();

        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
        assertNotEquals(message1, message3);
    }
}
