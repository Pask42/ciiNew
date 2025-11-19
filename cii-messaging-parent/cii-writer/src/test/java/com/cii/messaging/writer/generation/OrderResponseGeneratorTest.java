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
import com.cii.messaging.unece.order.LineTradeAgreementType;
import com.cii.messaging.unece.order.LineTradeDeliveryType;
import com.cii.messaging.unece.order.LineTradeSettlementType;
import com.cii.messaging.unece.order.QuantityType;
import com.cii.messaging.unece.order.SupplyChainTradeLineItemType;
import com.cii.messaging.unece.order.SupplyChainTradeTransactionType;
import com.cii.messaging.unece.order.TextType;
import com.cii.messaging.unece.order.TradePartyType;
import com.cii.messaging.unece.order.TradeProductType;
import com.cii.messaging.unece.order.TradeSettlementHeaderMonetarySummationType;
import com.cii.messaging.unece.order.TradeSettlementLineMonetarySummationType;
import com.cii.messaging.unece.order.TradePriceType;
import com.cii.messaging.reader.CIIReaderException;
import com.cii.messaging.reader.OrderReader;
import com.cii.messaging.writer.CIIWriterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
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
                .withAcknowledgementCode(DocumentStatusCodes.DEFAULT_ACKNOWLEDGEMENT_CODE)
                .build();

        OrderResponse response = OrderResponseGenerator.genererDepuisOrder(order, options);

        assertEquals("RSP-DOC-001", response.getExchangedDocument().getID().getValue());
        assertEquals(DocumentStatusCodes.DEFAULT_ACKNOWLEDGEMENT_CODE,
                response.getExchangedDocument().getStatusCode().getValue());
        assertEquals("6", response.getExchangedDocument().getStatusCode().getListAgencyID());
        assertEquals("6", response.getExchangedDocument().getTypeCode().getListAgencyID());
        assertEquals("20240305",
                response.getExchangedDocument().getIssueDateTime().getDateTimeString().getValue());
        assertEquals(1, response.getSupplyChainTradeTransaction().getIncludedSupplyChainTradeLineItem().size());
        com.cii.messaging.unece.orderresponse.SupplyChainTradeLineItemType lineItem =
                response.getSupplyChainTradeTransaction().getIncludedSupplyChainTradeLineItem().get(0);
        assertEquals(new BigDecimal("5"), lineItem.getSpecifiedLineTradeDelivery().getAgreedQuantity().getValue());
        assertEquals(new BigDecimal("50.00"), lineItem.getSpecifiedLineTradeAgreement()
                .getNetPriceProductTradePrice().getChargeAmount().get(0).getValue());
        assertEquals("EUR", lineItem.getSpecifiedLineTradeAgreement().getNetPriceProductTradePrice()
                .getChargeAmount().get(0).getCurrencyID());
        assertEquals(new BigDecimal("250.00"), lineItem.getSpecifiedLineTradeSettlement()
                .getSpecifiedTradeSettlementLineMonetarySummation().getLineTotalAmount().get(0).getValue());
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

    @Test
    void conserveLesPrixEtTotauxDepuisUnEchantillon() throws Exception {
        Order order = lireEchantillonOrder("/samples/AMAZON_OUT.xml");

        OrderResponse response = OrderResponseGenerator.genererDepuisOrder(order,
                OrderResponseGenerationOptions.builder()
                        .withIssueDateTime(LocalDateTime.of(2024, 3, 5, 12, 0))
                        .build());

        com.cii.messaging.unece.orderresponse.SupplyChainTradeLineItemType firstLine =
                response.getSupplyChainTradeTransaction().getIncludedSupplyChainTradeLineItem().get(0);

        assertEquals(new BigDecimal("20.78"), firstLine.getSpecifiedLineTradeAgreement()
                .getNetPriceProductTradePrice().getChargeAmount().get(0).getValue());
        assertEquals(new BigDecimal("103.9"), firstLine.getSpecifiedLineTradeSettlement()
                .getSpecifiedTradeSettlementLineMonetarySummation().getLineTotalAmount().get(0).getValue());
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
        dateTimeString.setValue("20240201");
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

        LineTradeAgreementType agreement = new LineTradeAgreementType();
        TradePriceType price = new TradePriceType();
        price.getChargeAmount().add(amount("50.00", "EUR"));
        agreement.setNetPriceProductTradePrice(price);
        line.setSpecifiedLineTradeAgreement(agreement);

        LineTradeDeliveryType delivery = new LineTradeDeliveryType();
        delivery.setRequestedQuantity(quantity("5", "EA"));
        line.setSpecifiedLineTradeDelivery(delivery);

        LineTradeSettlementType settlement = new LineTradeSettlementType();
        TradeSettlementLineMonetarySummationType lineSummation = new TradeSettlementLineMonetarySummationType();
        lineSummation.getLineTotalAmount().add(amount("250.00", "EUR"));
        settlement.setSpecifiedTradeSettlementLineMonetarySummation(lineSummation);
        line.setSpecifiedLineTradeSettlement(settlement);
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

    private Order lireEchantillonOrder(String resource) throws IOException, CIIReaderException {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            if (inputStream == null) {
                throw new IOException("Ressource introuvable : " + resource);
            }
            return new OrderReader().read(inputStream);
        }
    }
}
