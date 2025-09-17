package com.cii.messaging.writer.generation;

import com.cii.messaging.model.order.Order;
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
import com.cii.messaging.unece.order.TradePriceType;
import com.cii.messaging.unece.order.TradeProductType;
import com.cii.messaging.unece.order.TradeSettlementLineMonetarySummationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateOrdersXmlAndReturnConfirmationMessage() throws Exception {
        Order order = buildOrder();
        Path outputFile = tempDir.resolve("orders.xml");

        String message = OrderGenerator.genererOrders(() -> order, outputFile.toString());

        assertTrue(message.startsWith("Fichier ORDERS généré avec succès : "));
        assertTrue(message.contains(outputFile.toAbsolutePath().toString()));
        assertTrue(Files.exists(outputFile), "Le fichier ORDERS n'a pas été créé");

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("CrossIndustryOrder"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document;
        try (InputStream is = Files.newInputStream(outputFile)) {
            document = factory.newDocumentBuilder().parse(is);
        }
        XPath xpath = XPathFactory.newInstance().newXPath();

        assertEquals("CrossIndustryOrder", document.getDocumentElement().getLocalName());
        assertEquals("DOC-123", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='ExchangedDocument']/*[local-name()='ID']"));
        assertEquals("REF-001", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='ApplicableHeaderTradeAgreement']/*[local-name()='BuyerReference']"));
        assertEquals("EUR", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='ApplicableHeaderTradeSettlement']/*[local-name()='OrderCurrencyCode']"));
    }

    private static Order buildOrder() {
        Order order = new Order();

        ExchangedDocumentContextType context = new ExchangedDocumentContextType();
        context.setSpecifiedTransactionID(id("TRANS-123"));
        order.setExchangedDocumentContext(context);

        ExchangedDocumentType document = new ExchangedDocumentType();
        document.setID(id("DOC-123"));
        document.getName().add(text("Commande Test"));
        DateTimeType issueDate = new DateTimeType();
        DateTimeType.DateTimeString dateTimeString = new DateTimeType.DateTimeString();
        dateTimeString.setFormat("102");
        dateTimeString.setValue("20240210103000");
        issueDate.setDateTimeString(dateTimeString);
        document.setIssueDateTime(issueDate);
        order.setExchangedDocument(document);

        SupplyChainTradeTransactionType transaction = new SupplyChainTradeTransactionType();
        transaction.setApplicableHeaderTradeAgreement(buildHeaderAgreement());
        transaction.setApplicableHeaderTradeDelivery(buildHeaderDelivery());
        transaction.setApplicableHeaderTradeSettlement(buildHeaderSettlement());
        transaction.getIncludedSupplyChainTradeLineItem().add(buildLineItem());
        order.setSupplyChainTradeTransaction(transaction);

        return order;
    }

    private static HeaderTradeAgreementType buildHeaderAgreement() {
        HeaderTradeAgreementType agreement = new HeaderTradeAgreementType();
        agreement.setBuyerReference(text("REF-001"));

        TradePartyType seller = new TradePartyType();
        seller.getID().add(id("SELLER-123"));
        seller.setName(text("Vendeur Exemple"));
        agreement.setSellerTradeParty(seller);

        TradePartyType buyer = new TradePartyType();
        buyer.getID().add(id("BUYER-456"));
        buyer.setName(text("Acheteur Exemple"));
        agreement.setBuyerTradeParty(buyer);

        return agreement;
    }

    private static HeaderTradeDeliveryType buildHeaderDelivery() {
        HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();
        TradePartyType shipTo = new TradePartyType();
        shipTo.setName(text("Entrepôt Acheteur"));
        delivery.setShipToTradeParty(shipTo);
        return delivery;
    }

    private static HeaderTradeSettlementType buildHeaderSettlement() {
        HeaderTradeSettlementType settlement = new HeaderTradeSettlementType();
        settlement.setOrderCurrencyCode(currency("EUR"));
        settlement.getDuePayableAmount().add(amount("100.00", "EUR"));
        return settlement;
    }

    private static SupplyChainTradeLineItemType buildLineItem() {
        SupplyChainTradeLineItemType lineItem = new SupplyChainTradeLineItemType();

        DocumentLineDocumentType lineDocument = new DocumentLineDocumentType();
        lineDocument.setLineID(id("LINE-1"));
        lineItem.setAssociatedDocumentLineDocument(lineDocument);

        TradeProductType product = new TradeProductType();
        product.getName().add(text("Produit Exemple"));
        lineItem.setSpecifiedTradeProduct(product);

        LineTradeAgreementType lineAgreement = new LineTradeAgreementType();
        TradePriceType price = new TradePriceType();
        price.getChargeAmount().add(amount("100.00", "EUR"));
        lineAgreement.setNetPriceProductTradePrice(price);
        lineItem.setSpecifiedLineTradeAgreement(lineAgreement);

        LineTradeDeliveryType lineDelivery = new LineTradeDeliveryType();
        lineDelivery.setRequestedQuantity(quantity("1", "EA"));
        lineItem.setSpecifiedLineTradeDelivery(lineDelivery);

        LineTradeSettlementType lineSettlement = new LineTradeSettlementType();
        TradeSettlementLineMonetarySummationType summation = new TradeSettlementLineMonetarySummationType();
        summation.getLineTotalAmount().add(amount("100.00", "EUR"));
        lineSettlement.setSpecifiedTradeSettlementLineMonetarySummation(summation);
        lineItem.setSpecifiedLineTradeSettlement(lineSettlement);

        return lineItem;
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

    private static QuantityType quantity(String value, String unitCode) {
        QuantityType quantity = new QuantityType();
        quantity.setValue(new BigDecimal(value));
        quantity.setUnitCode(unitCode);
        return quantity;
    }

    private static CurrencyCodeType currency(String value) {
        CurrencyCodeType currency = new CurrencyCodeType();
        currency.setValue(ISO3AlphaCurrencyCodeContentType.valueOf(value));
        return currency;
    }

    private static String evaluate(XPath xpath, Document document, String expression) throws Exception {
        return xpath.evaluate(expression, document).trim();
    }
}
