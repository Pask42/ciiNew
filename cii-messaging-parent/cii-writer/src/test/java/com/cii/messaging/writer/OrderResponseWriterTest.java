package com.cii.messaging.writer;

import com.cii.messaging.model.orderresponse.OrderResponse;
import com.cii.messaging.unece.orderresponse.AmountType;
import com.cii.messaging.unece.orderresponse.CurrencyCodeType;
import com.cii.messaging.unece.orderresponse.DateTimeType;
import com.cii.messaging.unece.orderresponse.DocumentCodeType;
import com.cii.messaging.unece.orderresponse.ExchangedDocumentContextType;
import com.cii.messaging.unece.orderresponse.ExchangedDocumentType;
import com.cii.messaging.unece.orderresponse.HeaderTradeAgreementType;
import com.cii.messaging.unece.orderresponse.HeaderTradeDeliveryType;
import com.cii.messaging.unece.orderresponse.HeaderTradeSettlementType;
import com.cii.messaging.unece.orderresponse.IDType;
import com.cii.messaging.unece.orderresponse.ISO3AlphaCurrencyCodeContentType;
import com.cii.messaging.unece.orderresponse.SupplyChainTradeTransactionType;
import com.cii.messaging.unece.orderresponse.TextType;
import com.cii.messaging.unece.orderresponse.TradePartyType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderResponseWriterTest {

    @Test
    void doitExposerLesPrefixesCiiStandards() throws Exception {
        OrderResponse response = buildMinimalOrderResponse();

        CIIWriter<OrderResponse> writer = new OrderResponseWriter();
        String xml = writer.writeToString(response);

        assertTrue(xml.contains("xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:100\""));
        assertTrue(xml.contains("xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100\""));
        assertTrue(xml.contains("xmlns:udt=\"urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100\""));
        assertTrue(xml.contains("xmlns:qdt=\"urn:un:unece:uncefact:data:standard:QualifiedDataType:100\""));
    }

    private static OrderResponse buildMinimalOrderResponse() {
        OrderResponse response = new OrderResponse();

        ExchangedDocumentContextType context = new ExchangedDocumentContextType();
        context.setSpecifiedTransactionID(id("TRANS-RESP-001"));
        response.setExchangedDocumentContext(context);

        ExchangedDocumentType document = new ExchangedDocumentType();
        document.setID(id("RESP-2024-001"));
        document.setTypeCode(documentCode("231"));
        document.setIssueDateTime(dateTime("102", "20240101120000"));
        response.setExchangedDocument(document);

        SupplyChainTradeTransactionType transaction = new SupplyChainTradeTransactionType();
        transaction.setApplicableHeaderTradeAgreement(buildAgreement());
        transaction.setApplicableHeaderTradeDelivery(buildDelivery());
        transaction.setApplicableHeaderTradeSettlement(buildSettlement());
        response.setSupplyChainTradeTransaction(transaction);

        return response;
    }

    private static HeaderTradeAgreementType buildAgreement() {
        HeaderTradeAgreementType agreement = new HeaderTradeAgreementType();

        TradePartyType seller = new TradePartyType();
        seller.setName(text("Vendor"));
        agreement.setSellerTradeParty(seller);

        TradePartyType buyer = new TradePartyType();
        buyer.setName(text("Customer"));
        agreement.setBuyerTradeParty(buyer);

        return agreement;
    }

    private static HeaderTradeDeliveryType buildDelivery() {
        HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();
        TradePartyType shipTo = new TradePartyType();
        shipTo.setName(text("Delivery Location"));
        delivery.setShipToTradeParty(shipTo);
        return delivery;
    }

    private static HeaderTradeSettlementType buildSettlement() {
        HeaderTradeSettlementType settlement = new HeaderTradeSettlementType();

        CurrencyCodeType currency = new CurrencyCodeType();
        currency.setValue(ISO3AlphaCurrencyCodeContentType.EUR);
        settlement.setOrderCurrencyCode(currency);

        AmountType amount = new AmountType();
        amount.setValue(new BigDecimal("120.00"));
        amount.setCurrencyID("EUR");
        settlement.getDuePayableAmount().add(amount);

        return settlement;
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

    private static DateTimeType dateTime(String format, String value) {
        DateTimeType dateTime = new DateTimeType();
        DateTimeType.DateTimeString dateTimeString = new DateTimeType.DateTimeString();
        dateTimeString.setFormat(format);
        dateTimeString.setValue(value);
        dateTime.setDateTimeString(dateTimeString);
        return dateTime;
    }

    private static DocumentCodeType documentCode(String value) {
        DocumentCodeType code = new DocumentCodeType();
        code.setValue(value);
        return code;
    }
}
