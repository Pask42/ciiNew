package com.cii.messaging.writer.generation;

import com.cii.messaging.model.despatchadvice.DespatchAdvice;
import com.cii.messaging.unece.despatchadvice.CodeType;
import com.cii.messaging.unece.despatchadvice.DateTimeType;
import com.cii.messaging.unece.despatchadvice.DocumentCodeType;
import com.cii.messaging.unece.despatchadvice.DocumentLineDocumentType;
import com.cii.messaging.unece.despatchadvice.ExchangedDocumentContextType;
import com.cii.messaging.unece.despatchadvice.ExchangedDocumentType;
import com.cii.messaging.unece.despatchadvice.HeaderTradeAgreementType;
import com.cii.messaging.unece.despatchadvice.HeaderTradeDeliveryType;
import com.cii.messaging.unece.despatchadvice.IDType;
import com.cii.messaging.unece.despatchadvice.LineTradeDeliveryType;
import com.cii.messaging.unece.despatchadvice.QuantityType;
import com.cii.messaging.unece.despatchadvice.SupplyChainEventType;
import com.cii.messaging.unece.despatchadvice.SupplyChainTradeLineItemType;
import com.cii.messaging.unece.despatchadvice.SupplyChainTradeTransactionType;
import com.cii.messaging.unece.despatchadvice.TextType;
import com.cii.messaging.unece.despatchadvice.TradePartyType;
import com.cii.messaging.unece.despatchadvice.TradeProductType;
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

class DesadvGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateDesadvXmlAndReturnConfirmationMessage() throws Exception {
        DespatchAdvice despatchAdvice = buildDespatchAdvice();
        Path outputFile = tempDir.resolve("desadv.xml");

        String message = DesadvGenerator.genererDesadv(() -> despatchAdvice, outputFile.toString());

        assertTrue(message.startsWith("Fichier DESADV généré avec succès : "));
        assertTrue(message.contains(outputFile.toAbsolutePath().toString()));
        assertTrue(Files.exists(outputFile), "Le fichier DESADV n'a pas été créé");

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("CrossIndustryDespatchAdvice"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document;
        try (InputStream is = Files.newInputStream(outputFile)) {
            document = factory.newDocumentBuilder().parse(is);
        }
        XPath xpath = XPathFactory.newInstance().newXPath();

        assertEquals("CrossIndustryDespatchAdvice", document.getDocumentElement().getLocalName());
        assertEquals("DESADV-2025-001", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryDespatchAdvice']/*[local-name()='ExchangedDocument']/*[local-name()='ID']"));
        assertEquals("Expéditeur Logistique", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryDespatchAdvice']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='Applic"
                        + "ableHeaderTradeAgreement']/*[local-name()='SellerTradeParty']/*[local-name()='Name']"));
        assertEquals("Pompe hydraulique", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryDespatchAdvice']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='Included"
                        + "SupplyChainTradeLineItem']/*[local-name()='SpecifiedTradeProduct']/*[local-name()='Name']"));
        assertEquals("10", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryDespatchAdvice']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='Included"
                        + "SupplyChainTradeLineItem']/*[local-name()='SpecifiedLineTradeDelivery']/*[local-name()='DespatchedQuantity']"));
    }

    private static DespatchAdvice buildDespatchAdvice() {
        DespatchAdvice despatchAdvice = new DespatchAdvice();

        ExchangedDocumentContextType context = new ExchangedDocumentContextType();
        context.setSpecifiedTransactionID(id("TRANS-DES-001"));
        despatchAdvice.setExchangedDocumentContext(context);

        ExchangedDocumentType exchangedDocument = new ExchangedDocumentType();
        exchangedDocument.setID(id("DESADV-2025-001"));
        exchangedDocument.getName().add(text("Avis d'expédition"));
        exchangedDocument.setTypeCode(documentCode("351"));
        exchangedDocument.setIssueDateTime(dateTime("102", "20250116083000"));
        despatchAdvice.setExchangedDocument(exchangedDocument);

        SupplyChainTradeTransactionType transaction = new SupplyChainTradeTransactionType();
        transaction.getShipmentID().add(id("SHIP-2025-01"));
        transaction.setApplicableHeaderTradeAgreement(buildHeaderAgreement());
        transaction.setApplicableHeaderTradeDelivery(buildHeaderDelivery());
        transaction.getIncludedSupplyChainTradeLineItem().add(buildLineItem());
        despatchAdvice.setSupplyChainTradeTransaction(transaction);

        return despatchAdvice;
    }

    private static HeaderTradeAgreementType buildHeaderAgreement() {
        HeaderTradeAgreementType agreement = new HeaderTradeAgreementType();
        agreement.setSellerTradeParty(tradeParty("SELLER-01", "Expéditeur Logistique"));
        agreement.setBuyerTradeParty(tradeParty("BUYER-01", "Client Industriel"));
        return agreement;
    }

    private static HeaderTradeDeliveryType buildHeaderDelivery() {
        HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();
        delivery.setShipFromTradeParty(tradeParty("FROM-01", "Entrepôt Nord"));
        delivery.setShipToTradeParty(tradeParty("TO-99", "Usine Client"));

        SupplyChainEventType despatchEvent = new SupplyChainEventType();
        despatchEvent.setID(id("EVENT-01"));
        despatchEvent.setOccurrenceDateTime(dateTime("102", "20250116103000"));
        despatchEvent.setTypeCode(code("85"));
        delivery.setActualDespatchSupplyChainEvent(despatchEvent);

        return delivery;
    }

    private static SupplyChainTradeLineItemType buildLineItem() {
        SupplyChainTradeLineItemType lineItem = new SupplyChainTradeLineItemType();
        lineItem.setID(id("LINE-100"));

        DocumentLineDocumentType lineDocument = new DocumentLineDocumentType();
        lineDocument.setLineID(id("LINE-100"));
        lineItem.setAssociatedDocumentLineDocument(lineDocument);

        TradeProductType product = new TradeProductType();
        product.getName().add(text("Pompe hydraulique"));
        lineItem.getSpecifiedTradeProduct().add(product);

        LineTradeDeliveryType lineDelivery = new LineTradeDeliveryType();
        lineDelivery.setDespatchedQuantity(quantity("10", "EA"));
        lineItem.getSpecifiedLineTradeDelivery().add(lineDelivery);

        return lineItem;
    }

    private static TradePartyType tradeParty(String idValue, String name) {
        TradePartyType party = new TradePartyType();
        party.getID().add(id(idValue));
        party.setName(text(name));
        return party;
    }

    private static DocumentCodeType documentCode(String value) {
        DocumentCodeType type = new DocumentCodeType();
        type.setValue(value);
        return type;
    }

    private static CodeType code(String value) {
        CodeType code = new CodeType();
        code.setValue(value);
        return code;
    }

    private static DateTimeType dateTime(String format, String value) {
        DateTimeType dateTime = new DateTimeType();
        DateTimeType.DateTimeString dateTimeString = new DateTimeType.DateTimeString();
        dateTimeString.setFormat(format);
        dateTimeString.setValue(value);
        dateTime.setDateTimeString(dateTimeString);
        return dateTime;
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

    private static QuantityType quantity(String value, String unitCode) {
        QuantityType quantity = new QuantityType();
        quantity.setValue(new BigDecimal(value));
        quantity.setUnitCode(unitCode);
        return quantity;
    }

    private static String evaluate(XPath xpath, Document document, String expression) throws Exception {
        return xpath.evaluate(expression, document).trim();
    }
}
