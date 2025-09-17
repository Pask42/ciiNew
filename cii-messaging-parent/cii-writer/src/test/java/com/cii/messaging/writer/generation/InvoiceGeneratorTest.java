package com.cii.messaging.writer.generation;

import com.cii.messaging.model.invoice.Invoice;
import com.cii.messaging.unece.invoice.AmountType;
import com.cii.messaging.unece.invoice.CodeType;
import com.cii.messaging.unece.invoice.CountryIDType;
import com.cii.messaging.unece.invoice.CurrencyCodeType;
import com.cii.messaging.unece.invoice.DateTimeType;
import com.cii.messaging.unece.invoice.DocumentCodeType;
import com.cii.messaging.unece.invoice.DocumentLineDocumentType;
import com.cii.messaging.unece.invoice.DutyTaxFeeTypeCodeContentType;
import com.cii.messaging.unece.invoice.DutyorTaxorFeeCategoryCodeContentType;
import com.cii.messaging.unece.invoice.ExchangedDocumentContextType;
import com.cii.messaging.unece.invoice.ExchangedDocumentType;
import com.cii.messaging.unece.invoice.HeaderTradeAgreementType;
import com.cii.messaging.unece.invoice.HeaderTradeDeliveryType;
import com.cii.messaging.unece.invoice.HeaderTradeSettlementType;
import com.cii.messaging.unece.invoice.IDType;
import com.cii.messaging.unece.invoice.ISO3AlphaCurrencyCodeContentType;
import com.cii.messaging.unece.invoice.ISOTwoletterCountryCodeContentType;
import com.cii.messaging.unece.invoice.LineTradeAgreementType;
import com.cii.messaging.unece.invoice.LineTradeDeliveryType;
import com.cii.messaging.unece.invoice.LineTradeSettlementType;
import com.cii.messaging.unece.invoice.PercentType;
import com.cii.messaging.unece.invoice.QuantityType;
import com.cii.messaging.unece.invoice.SupplyChainEventType;
import com.cii.messaging.unece.invoice.SupplyChainTradeLineItemType;
import com.cii.messaging.unece.invoice.SupplyChainTradeTransactionType;
import com.cii.messaging.unece.invoice.TaxCategoryCodeType;
import com.cii.messaging.unece.invoice.TaxRegistrationType;
import com.cii.messaging.unece.invoice.TaxTypeCodeType;
import com.cii.messaging.unece.invoice.TextType;
import com.cii.messaging.unece.invoice.TradeAddressType;
import com.cii.messaging.unece.invoice.TradeContactType;
import com.cii.messaging.unece.invoice.TradePartyType;
import com.cii.messaging.unece.invoice.TradePaymentTermsType;
import com.cii.messaging.unece.invoice.TradePriceType;
import com.cii.messaging.unece.invoice.TradeProductType;
import com.cii.messaging.unece.invoice.TradeSettlementHeaderMonetarySummationType;
import com.cii.messaging.unece.invoice.TradeSettlementLineMonetarySummationType;
import com.cii.messaging.unece.invoice.TradeTaxType;
import com.cii.messaging.unece.invoice.UniversalCommunicationType;
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

class InvoiceGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateInvoiceXmlAndReturnConfirmationMessage() throws Exception {
        Invoice invoice = buildInvoice();
        Path outputFile = tempDir.resolve("invoice.xml");

        String message = InvoiceGenerator.genererInvoice(() -> invoice, outputFile.toString());

        assertTrue(message.startsWith("Fichier INVOICE généré avec succès : "));
        assertTrue(message.contains(outputFile.toAbsolutePath().toString()));
        assertTrue(Files.exists(outputFile), "Le fichier INVOICE n'a pas été créé");

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("CrossIndustryInvoice"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document;
        try (InputStream is = Files.newInputStream(outputFile)) {
            document = factory.newDocumentBuilder().parse(is);
        }
        XPath xpath = XPathFactory.newInstance().newXPath();

        assertEquals("CrossIndustryInvoice", document.getDocumentElement().getLocalName());
        assertEquals("INV-2025-0001", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryInvoice']/*[local-name()='ExchangedDocument']/*[local-name()='ID']"));
        assertEquals("Fournisseur Industriel", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryInvoice']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='ApplicableHead"
                        + "erTradeAgreement']/*[local-name()='SellerTradeParty']/*[local-name()='Name']"));
        assertEquals("GTIN", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryInvoice']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='IncludedSupply"
                        + "ChainTradeLineItem']/*[local-name()='SpecifiedTradeProduct']/*[local-name()='GlobalID']/@schemeID"));
        assertEquals("1200.00", evaluate(xpath, document,
                "/*[local-name()='CrossIndustryInvoice']/*[local-name()='SupplyChainTradeTransaction']/*[local-name()='ApplicableHeade"
                        + "rTradeSettlement']/*[local-name()='DuePayableAmount']"));
    }

    private static Invoice buildInvoice() {
        Invoice invoice = new Invoice();

        ExchangedDocumentContextType context = new ExchangedDocumentContextType();
        context.setSpecifiedTransactionID(id("TRANS-INV-2025-01"));
        invoice.setExchangedDocumentContext(context);

        invoice.setExchangedDocument(buildExchangedDocument());
        invoice.setSupplyChainTradeTransaction(buildTransaction());

        return invoice;
    }

    private static ExchangedDocumentType buildExchangedDocument() {
        ExchangedDocumentType document = new ExchangedDocumentType();
        document.setID(id("INV-2025-0001"));
        document.getName().add(text("Facture client"));
        document.setTypeCode(documentCode("380"));
        document.setIssueDateTime(dateTime("102", "20250301101500"));
        return document;
    }

    private static SupplyChainTradeTransactionType buildTransaction() {
        SupplyChainTradeTransactionType transaction = new SupplyChainTradeTransactionType();
        transaction.setApplicableHeaderTradeAgreement(buildHeaderAgreement());
        transaction.setApplicableHeaderTradeDelivery(buildHeaderDelivery());
        transaction.setApplicableHeaderTradeSettlement(buildHeaderSettlement());
        transaction.getIncludedSupplyChainTradeLineItem().add(buildLineItem());
        return transaction;
    }

    private static HeaderTradeAgreementType buildHeaderAgreement() {
        HeaderTradeAgreementType agreement = new HeaderTradeAgreementType();
        agreement.setBuyerReference(text("PO-78945"));
        agreement.setSellerTradeParty(buildSeller());
        agreement.setBuyerTradeParty(buildBuyer());
        return agreement;
    }

    private static TradePartyType buildSeller() {
        TradePartyType seller = new TradePartyType();
        seller.getID().add(id("SEL-001"));
        seller.setName(text("Fournisseur Industriel"));
        seller.setPostalTradeAddress(address("75008", "10 Rue des Forges", "Paris", "FR"));
        seller.getSpecifiedTaxRegistration().add(taxRegistration("FR123456789", "VA"));

        TradeContactType contact = new TradeContactType();
        contact.setPersonName(text("Alice Fournisseur"));
        contact.setTelephoneUniversalCommunication(communication(null, "+33 1 02 03 04 05"));
        contact.setEmailURIUniversalCommunication(communication("alice.fournisseur@supplier.fr", null));
        seller.getDefinedTradeContact().add(contact);

        return seller;
    }

    private static TradePartyType buildBuyer() {
        TradePartyType buyer = new TradePartyType();
        buyer.getID().add(id("BUY-009"));
        buyer.setName(text("Client Services"));
        buyer.setPostalTradeAddress(address("69002", "25 Avenue Centrale", "Lyon", "FR"));
        return buyer;
    }

    private static HeaderTradeDeliveryType buildHeaderDelivery() {
        HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();

        SupplyChainEventType deliveryEvent = new SupplyChainEventType();
        deliveryEvent.setID(id("EVT-DELIVERY-2025"));
        deliveryEvent.setOccurrenceDateTime(dateTime("102", "20250227"));
        delivery.setActualDeliverySupplyChainEvent(deliveryEvent);

        return delivery;
    }

    private static HeaderTradeSettlementType buildHeaderSettlement() {
        HeaderTradeSettlementType settlement = new HeaderTradeSettlementType();
        settlement.setInvoiceCurrencyCode(currency("EUR"));
        settlement.getDuePayableAmount().add(amount("1200.00", "EUR"));
        settlement.getSpecifiedTradePaymentTerms().add(paymentTerms());

        TradeSettlementHeaderMonetarySummationType summation = new TradeSettlementHeaderMonetarySummationType();
        summation.getLineTotalAmount().add(amount("1000.00", "EUR"));
        summation.getTaxBasisTotalAmount().add(amount("1000.00", "EUR"));
        summation.getTaxTotalAmount().add(amount("200.00", "EUR"));
        summation.getGrandTotalAmount().add(amount("1200.00", "EUR"));
        summation.getDuePayableAmount().add(amount("1200.00", "EUR"));
        settlement.setSpecifiedTradeSettlementHeaderMonetarySummation(summation);

        return settlement;
    }

    private static TradePaymentTermsType paymentTerms() {
        TradePaymentTermsType terms = new TradePaymentTermsType();
        terms.getDescription().add(text("Paiement à 30 jours"));
        terms.setDueDateDateTime(dateTime("102", "20250331"));
        return terms;
    }

    private static SupplyChainTradeLineItemType buildLineItem() {
        SupplyChainTradeLineItemType lineItem = new SupplyChainTradeLineItemType();

        DocumentLineDocumentType lineDocument = new DocumentLineDocumentType();
        lineDocument.setLineID(id("LINE-01"));
        lineItem.setAssociatedDocumentLineDocument(lineDocument);

        TradeProductType product = new TradeProductType();
        product.setGlobalID(id("4012345000001", "GTIN"));
        product.getName().add(text("Pompe haute pression"));
        lineItem.setSpecifiedTradeProduct(product);

        LineTradeAgreementType lineAgreement = new LineTradeAgreementType();
        TradePriceType price = new TradePriceType();
        price.getChargeAmount().add(amount("250.00", "EUR"));
        lineAgreement.setNetPriceProductTradePrice(price);
        lineItem.setSpecifiedLineTradeAgreement(lineAgreement);

        LineTradeDeliveryType lineDelivery = new LineTradeDeliveryType();
        lineDelivery.setBilledQuantity(quantity("4", "EA"));
        lineItem.setSpecifiedLineTradeDelivery(lineDelivery);

        LineTradeSettlementType lineSettlement = new LineTradeSettlementType();
        TradeTaxType tax = new TradeTaxType();
        tax.getCalculatedAmount().add(amount("200.00", "EUR"));
        tax.getBasisAmount().add(amount("1000.00", "EUR"));
        TaxTypeCodeType taxTypeCode = new TaxTypeCodeType();
        taxTypeCode.setValue(DutyTaxFeeTypeCodeContentType.VAT);
        tax.setTypeCode(taxTypeCode);
        TaxCategoryCodeType taxCategory = new TaxCategoryCodeType();
        taxCategory.setValue(DutyorTaxorFeeCategoryCodeContentType.S);
        tax.setCategoryCode(taxCategory);
        PercentType rate = new PercentType();
        rate.setValue(new BigDecimal("20"));
        tax.setRateApplicablePercent(rate);
        lineSettlement.getApplicableTradeTax().add(tax);

        TradeSettlementLineMonetarySummationType lineSummation = new TradeSettlementLineMonetarySummationType();
        lineSummation.getLineTotalAmount().add(amount("1000.00", "EUR"));
        lineSummation.getTaxTotalAmount().add(amount("200.00", "EUR"));
        lineSummation.getGrandTotalAmount().add(amount("1200.00", "EUR"));
        lineSettlement.setSpecifiedTradeSettlementLineMonetarySummation(lineSummation);

        lineItem.setSpecifiedLineTradeSettlement(lineSettlement);

        return lineItem;
    }

    private static TradeAddressType address(String postcode, String lineOne, String city, String countryCode) {
        TradeAddressType address = new TradeAddressType();
        address.setPostcodeCode(code(postcode));
        address.setLineOne(text(lineOne));
        address.setCityName(text(city));
        CountryIDType country = new CountryIDType();
        country.setValue(ISOTwoletterCountryCodeContentType.valueOf(countryCode));
        address.setCountryID(country);
        return address;
    }

    private static TaxRegistrationType taxRegistration(String idValue, String schemeId) {
        TaxRegistrationType registration = new TaxRegistrationType();
        registration.setID(id(idValue, schemeId));
        return registration;
    }

    private static UniversalCommunicationType communication(String uri, String number) {
        UniversalCommunicationType communication = new UniversalCommunicationType();
        if (uri != null) {
            communication.setURIID(id(uri));
        }
        if (number != null) {
            communication.setCompleteNumber(text(number));
        }
        return communication;
    }

    private static DocumentCodeType documentCode(String value) {
        DocumentCodeType type = new DocumentCodeType();
        type.setValue(value);
        return type;
    }

    private static CurrencyCodeType currency(String currency) {
        CurrencyCodeType currencyCode = new CurrencyCodeType();
        currencyCode.setValue(ISO3AlphaCurrencyCodeContentType.valueOf(currency));
        return currencyCode;
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

    private static IDType id(String value, String schemeId) {
        IDType id = id(value);
        id.setSchemeID(schemeId);
        return id;
    }

    private static TextType text(String value) {
        TextType text = new TextType();
        text.setValue(value);
        return text;
    }

    private static CodeType code(String value) {
        CodeType code = new CodeType();
        code.setValue(value);
        return code;
    }

    private static String evaluate(XPath xpath, Document document, String expression) throws Exception {
        return xpath.evaluate(expression, document).trim();
    }
}
