package com.cii.messaging.writer.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.orderresponse.OrderResponse;
import com.cii.messaging.writer.CIIWriterException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.time.format.DateTimeFormatter;

import com.cii.messaging.unece.orderresponse.DateTimeType;
import com.cii.messaging.unece.orderresponse.ExchangedDocumentContextType;
import com.cii.messaging.unece.orderresponse.ExchangedDocumentType;
import com.cii.messaging.unece.orderresponse.IDType;
import com.cii.messaging.unece.orderresponse.SupplyChainTradeTransactionType;

/**
 * Writer implementation using JAXB-generated OrderResponse model.
 */
public class OrderResponseWriter extends AbstractCIIWriter {

    @Override
    protected void initializeJAXBContext() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(OrderResponse.class);
    }

    @Override
    protected Object createDocument(CIIMessage message) throws CIIWriterException {
        OrderResponse response = new OrderResponse();
        response.setExchangedDocumentContext(new ExchangedDocumentContextType());

        ExchangedDocumentType doc = new ExchangedDocumentType();
        IDType id = new IDType();
        id.setValue(message.getMessageId());
        doc.setID(id);

        DateTimeType dateTime = new DateTimeType();
        DateTimeType.DateTimeString dts = new DateTimeType.DateTimeString();
        dts.setFormat("102");
        if (message.getCreationDateTime() != null) {
            dts.setValue(message.getCreationDateTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        dateTime.setDateTimeString(dts);
        doc.setIssueDateTime(dateTime);
        response.setExchangedDocument(doc);

        response.setSupplyChainTradeTransaction(new SupplyChainTradeTransactionType());
        return response;
    }
}
