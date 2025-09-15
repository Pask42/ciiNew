package com.cii.messaging.writer.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.order.Order;
import com.cii.messaging.writer.CIIWriterException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.time.format.DateTimeFormatter;

import com.cii.messaging.unece.order.DateTimeType;
import com.cii.messaging.unece.order.ExchangedDocumentContextType;
import com.cii.messaging.unece.order.ExchangedDocumentType;
import com.cii.messaging.unece.order.IDType;
import com.cii.messaging.unece.order.SupplyChainTradeTransactionType;

/**
 * Writer implementation using JAXB-generated Order model.
 */
public class OrderWriter extends AbstractCIIWriter {

    @Override
    protected void initializeJAXBContext() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(Order.class);
    }

    @Override
    protected Object createDocument(CIIMessage message) throws CIIWriterException {
        Order order = new Order();
        order.setExchangedDocumentContext(new ExchangedDocumentContextType());

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
        order.setExchangedDocument(doc);

        order.setSupplyChainTradeTransaction(new SupplyChainTradeTransactionType());
        return order;
    }
}
