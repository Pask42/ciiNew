package com.cii.messaging.writer.generation;

import com.cii.messaging.model.order.Order;
import com.cii.messaging.model.orderresponse.OrderResponse;
import com.cii.messaging.unece.order.AmountType;
import com.cii.messaging.unece.order.CurrencyCodeType;
import com.cii.messaging.unece.order.DateTimeType;
import com.cii.messaging.unece.order.DocumentLineDocumentType;
import com.cii.messaging.unece.order.ExchangedDocumentContextType;
import com.cii.messaging.unece.order.ExchangedDocumentType;
import com.cii.messaging.unece.order.HeaderTradeAgreementType;
import com.cii.messaging.unece.order.HeaderTradeDeliveryType;
import com.cii.messaging.unece.order.HeaderTradeSettlementType;
import com.cii.messaging.unece.order.IDType;
import com.cii.messaging.unece.order.ISO3AlphaCurrencyCodeContentType;
import com.cii.messaging.unece.order.LineTradeDeliveryType;
import com.cii.messaging.unece.order.QuantityType;
import com.cii.messaging.unece.order.SupplyChainTradeLineItemType;
import com.cii.messaging.unece.order.SupplyChainTradeTransactionType;
import com.cii.messaging.unece.order.TextType;
import com.cii.messaging.unece.order.TradePartyType;
import com.cii.messaging.unece.order.TradeProductType;
import com.cii.messaging.unece.order.TradeSettlementHeaderMonetarySummationType;
import com.cii.messaging.writer.CIIWriterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderResponseGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void transformeCommandeEnOrderResponse() {
        Order order = buildOrder();

        OrderResponseGenerationOptions options = OrderResponseGenerationOptions.builder()
                .withResponseIdPrefix("RSP-")
                .withIssueDateTime(LocalDateTime.of(2024, 3, 5, 12, 0))
                .withAcknowledgementCode("AP")
                .build();

        OrderResponse response = OrderResponseGenerator.genererDepuisOrder(order, options);

        assertEquals("RSP-DOC-001", response.getExchangedDocument().getID().getValue());
        assertEquals("AP", response.getExchangedDocument().getStatusCode().getValue());
        assertEquals("20240305120000",
                response.getExchangedDocument().getIssueDateTime().getDateTimeString().getValue());
        assertEquals(1, response.getSupplyChainTradeTransaction().getIncludedSupplyChainTradeLineItem().size());
        assertEquals(new BigDecimal("5"), response.getSupplyChainTradeTransaction().getIncludedSupplyChainTradeLineItem().get(0)
                .getSpecifiedLineTradeDelivery().getAgreedQuantity().getValue());
        assertEquals("DOC-001", response.getSupplyChainTradeTransaction().getApplicableHeaderTradeAgreement()
                .getSellerOrderReferencedDocument().getIssuerAssignedID().getValue());
        assertEquals(1, response.getSupplyChainTradeTransaction().getApplicableHeaderTradeSettlement()
                .getSpecifiedTradeSettlementHeaderMonetarySummation().getGrandTotalAmount().size());
    }

    @Test
    void ecritOrderResponseSurDisque() throws IOException, CIIWriterException {
        Order order = buildOrder();
        Path output = tempDir.resolve("ordersp.xml");

        String message = OrderResponseGenerator.genererOrderResponse(order, output.toString(),
                OrderResponseGenerationOptions.builder()
                        .withIssueDateTime(LocalDateTime.of(2024, 3, 5, 12, 0))
                        .build());

        assertTrue(Files.exists(output));
        assertTrue(message.contains(output.toAbsolutePath().toString()));
        String content = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(content.contains("CrossIndustryOrderResponse"));
    }

    private static Order buildOrder() {
        Order order = new Order();

        ExchangedDocumentContextType context = new ExchangedDocumentContextType();
        context.setSpecifiedTransactionID(id("TRANS-001"));
        order.setExchangedDocumentContext(context);

        ExchangedDocumentType document = new ExchangedDocumentType();
        document.setID(id("DOC-001"));
        DateTimeType dateTime = new DateTimeType();
        DateTimeType.DateTimeString dateTimeString = new DateTimeType.DateTimeString();
        dateTimeString.setFormat("102");
        dateTimeString.setValue("20240201093000");
        dateTime.setDateTimeString(dateTimeString);
        document.setIssueDateTime(dateTime);
        document.getName().add(text("Commande Test"));
        order.setExchangedDocument(document);

        SupplyChainTradeTransactionType transaction = new SupplyChainTradeTransactionType();
        transaction.setApplicableHeaderTradeAgreement(buildAgreement());
        transaction.setApplicableHeaderTradeDelivery(buildDelivery());
        transaction.setApplicableHeaderTradeSettlement(buildSettlement());
        transaction.getIncludedSupplyChainTradeLineItem().add(buildLine());
        order.setSupplyChainTradeTransaction(transaction);

        return order;
    }

    private static HeaderTradeAgreementType buildAgreement() {
        HeaderTradeAgreementType agreement = new HeaderTradeAgreementType();
        agreement.setBuyerReference(text("BUY-REF"));

        TradePartyType seller = new TradePartyType();
        seller.getID().add(id("SELLER-001"));
        seller.setName(text("Fournisseur"));
        agreement.setSellerTradeParty(seller);

        TradePartyType buyer = new TradePartyType();
        buyer.getID().add(id("BUYER-001"));
        buyer.setName(text("Client"));
        agreement.setBuyerTradeParty(buyer);

        return agreement;
    }

    private static HeaderTradeDeliveryType buildDelivery() {
        HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();
        TradePartyType shipTo = new TradePartyType();
        shipTo.setName(text("Entrepôt Client"));
        delivery.setShipToTradeParty(shipTo);
        return delivery;
    }

    private static HeaderTradeSettlementType buildSettlement() {
        HeaderTradeSettlementType settlement = new HeaderTradeSettlementType();
        CurrencyCodeType currency = new CurrencyCodeType();
        currency.setValue(ISO3AlphaCurrencyCodeContentType.EUR);
        settlement.setOrderCurrencyCode(currency);

        TradeSettlementHeaderMonetarySummationType summation = new TradeSettlementHeaderMonetarySummationType();
        summation.getGrandTotalAmount().add(amount("250.00", "EUR"));
        settlement.setSpecifiedTradeSettlementHeaderMonetarySummation(summation);
        settlement.getDuePayableAmount().add(amount("250.00", "EUR"));
        return settlement;
    }

    private static SupplyChainTradeLineItemType buildLine() {
        SupplyChainTradeLineItemType line = new SupplyChainTradeLineItemType();

        DocumentLineDocumentType lineDoc = new DocumentLineDocumentType();
        lineDoc.setLineID(id("1"));
        line.setAssociatedDocumentLineDocument(lineDoc);

        TradeProductType product = new TradeProductType();
        product.getName().add(text("Produit"));
        line.setSpecifiedTradeProduct(product);

        LineTradeDeliveryType delivery = new LineTradeDeliveryType();
        delivery.setRequestedQuantity(quantity("5", "EA"));
        line.setSpecifiedLineTradeDelivery(delivery);
        return line;
    }

    private static IDType id(String value) {
        IDType id = new IDType();
        id.setValue(value);
        return id;
    }

    private static TextType text(String value) {
        TextType text = new TextType();
        text.setValue(value);
        return text;
    }

    private static AmountType amount(String value, String currency) {
        AmountType amount = new AmountType();
        amount.setValue(new BigDecimal(value));
        amount.setCurrencyID(currency);
        return amount;
    }

    private static QuantityType quantity(String value, String unit) {
        QuantityType quantity = new QuantityType();
        quantity.setValue(new BigDecimal(value));
        quantity.setUnitCode(unit);
        return quantity;
    }
}
