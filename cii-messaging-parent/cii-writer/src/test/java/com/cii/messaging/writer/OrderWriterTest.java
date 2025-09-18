package com.cii.messaging.writer;

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
import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderWriterTest {

    @Test
    void shouldWriteOrder() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/order-sample.xml")) {
            Order order = (Order) JAXBContext.newInstance(Order.class)
                    .createUnmarshaller().unmarshal(is);
            CIIWriter<Order> writer = new OrderWriter();
            String xml = writer.writeToString(order);
            assertTrue(xml.contains("CrossIndustryOrder"));
        }
    }

    @Test
    void shouldSerializeOrderWithExpectedXmlStructure() throws Exception {
        Order order = buildOrder();

        CIIWriter<Order> writer = new OrderWriter();
        String xml = writer.writeToString(order);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        assertEquals("CrossIndustryOrder", document.getDocumentElement().getLocalName());
        assertEquals("urn:un:unece:uncefact:data:standard:CrossIndustryOrder:100",
                document.getDocumentElement().getNamespaceURI());

        XPath xpath = XPathFactory.newInstance().newXPath();

        assertEquals("TRANS-001", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='ExchangedDocumentContext']/*[local-name()='SpecifiedTransactionID']"));
        assertEquals("DOC-456", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='ExchangedDocument']/*[local-name()='ID']"));
        assertEquals("20250115133000", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='ExchangedDocument']/*[local-name()='IssueDateTime']/*[local-name()='DateTimeString']"));
        assertEquals("REF-789", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='ApplicableHeaderTradeAgreement']/*[local-name()='BuyerReference']"));
        assertEquals("LINE-789", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupplyChainTradeLineItem']/*[local-name()='AssociatedDocumentLineDocument']/*[local-name()='LineID']"));
        assertEquals("Produit Test", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupplyChainTradeLineItem']/*[local-name()='SpecifiedTradeProduct']/*[local-name()='Name']"));
        assertEquals("5", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupplyChainTradeLineItem']/*[local-name()='SpecifiedLineTradeDelivery']/*[local-name()='RequestedQuantity']"));
        assertEquals("EA", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupplyChainTradeLineItem']/*[local-name()='SpecifiedLineTradeDelivery']/*[local-name()='RequestedQuantity']/@unitCode"));
        assertEquals("125.50", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupplyChainTradeLineItem']/*[local-name()='SpecifiedLineTradeAgreement']/*[local-name()='NetPriceProductTradePrice']/*[local-name()='ChargeAmount']"));
        assertEquals("EUR", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupplyChainTradeLineItem']/*[local-name()='SpecifiedLineTradeAgreement']/*[local-name()='NetPriceProductTradePrice']/*[local-name()='ChargeAmount']/@currencyID"));
        assertEquals("627.50", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupplyChainTradeLineItem']/*[local-name()='SpecifiedLineTradeSettlement']/*[local-name()='SpecifiedTradeSettlementLineMonetarySummation']/*[local-name()='LineTotalAmount']"));
        assertEquals("EUR", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupplyChainTradeLineItem']/*[local-name()='SpecifiedLineTradeSettlement']/*[local-name()='SpecifiedTradeSettlementLineMonetarySummation']/*[local-name()='LineTotalAmount']/@currencyID"));
        assertEquals("627.50", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='ApplicableHeaderTradeSettlement']/*[local-name()='DuePayableAmount']"));
        assertEquals("EUR", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryOrder']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='ApplicableHeaderTradeSettlement']/*[local-name()='DuePayableAmount']/@currencyID"));
    }

    private static Order buildOrder() {
        Order order = new Order();

        ExchangedDocumentContextType context = new ExchangedDocumentContextType();
        context.setSpecifiedTransactionID(id("TRANS-001"));
        order.setExchangedDocumentContext(context);

        ExchangedDocumentType exchangedDocument = new ExchangedDocumentType();
        exchangedDocument.setID(id("DOC-456"));
        exchangedDocument.getName().add(text("Commande d'achat"));
        DateTimeType issueDate = new DateTimeType();
        DateTimeType.DateTimeString dateTimeString = new DateTimeType.DateTimeString();
        dateTimeString.setFormat("102");
        dateTimeString.setValue("20250115133000");
        issueDate.setDateTimeString(dateTimeString);
        exchangedDocument.setIssueDateTime(issueDate);
        order.setExchangedDocument(exchangedDocument);

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
        agreement.setBuyerReference(text("REF-789"));

        TradePartyType seller = new TradePartyType();
        seller.getID().add(id("SELLER-01"));
        seller.setName(text("Vendeur SA"));
        agreement.setSellerTradeParty(seller);

        TradePartyType buyer = new TradePartyType();
        buyer.getID().add(id("BUYER-99"));
        buyer.setName(text("Client SARL"));
        agreement.setBuyerTradeParty(buyer);

        return agreement;
    }

    private static HeaderTradeDeliveryType buildHeaderDelivery() {
        HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();
        TradePartyType shipTo = new TradePartyType();
        shipTo.setName(text("Entrepôt Client"));
        delivery.setShipToTradeParty(shipTo);
        return delivery;
    }

    private static HeaderTradeSettlementType buildHeaderSettlement() {
        HeaderTradeSettlementType settlement = new HeaderTradeSettlementType();
        settlement.setOrderCurrencyCode(currency("EUR"));
        settlement.getDuePayableAmount().add(amount("627.50", "EUR"));
        return settlement;
    }

    private static SupplyChainTradeLineItemType buildLineItem() {
        SupplyChainTradeLineItemType lineItem = new SupplyChainTradeLineItemType();

        DocumentLineDocumentType lineDocument = new DocumentLineDocumentType();
        lineDocument.setLineID(id("LINE-789"));
        lineItem.setAssociatedDocumentLineDocument(lineDocument);

        TradeProductType product = new TradeProductType();
        product.getName().add(text("Produit Test"));
        lineItem.setSpecifiedTradeProduct(product);

        LineTradeAgreementType lineAgreement = new LineTradeAgreementType();
        TradePriceType price = new TradePriceType();
        price.getChargeAmount().add(amount("125.50", "EUR"));
        lineAgreement.setNetPriceProductTradePrice(price);
        lineItem.setSpecifiedLineTradeAgreement(lineAgreement);

        LineTradeDeliveryType lineDelivery = new LineTradeDeliveryType();
        lineDelivery.setRequestedQuantity(quantity("5", "EA"));
        lineItem.setSpecifiedLineTradeDelivery(lineDelivery);

        LineTradeSettlementType lineSettlement = new LineTradeSettlementType();
        TradeSettlementLineMonetarySummationType summation = new TradeSettlementLineMonetarySummationType();
        summation.getLineTotalAmount().add(amount("627.50", "EUR"));
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
