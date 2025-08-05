package com.cii.messaging.reader;

import com.cii.messaging.model.MessageType;
import com.cii.messaging.reader.impl.DesadvReader;
import com.cii.messaging.reader.impl.InvoiceReader;
import com.cii.messaging.reader.impl.OrderResponseReader;

public class CIIReaderFactory {

    public static CIIReader createReader(MessageType messageType) {
        switch (messageType) {
            case ORDER:
                return new com.cii.messaging.reader.impl.OrderReader();
            case INVOICE:
                return new InvoiceReader();
            case DESADV:
                return new DesadvReader();
            case ORDERSP:
                return new OrderResponseReader();
            default:
                throw new IllegalArgumentException("Unsupported message type: " + messageType);
        }
    }

    public static CIIReader createReader(String xmlContent) throws CIIReaderException {
        // Auto-detect message type from XML content
        if (xmlContent.contains("CrossIndustryOrder")) {
            return new com.cii.messaging.reader.impl.OrderReader();
        } else if (xmlContent.contains("CrossIndustryInvoice")) {
            return new InvoiceReader();
        } else if (xmlContent.contains("CrossIndustryDespatchAdvice")) {
            return new DesadvReader();
        } else if (xmlContent.contains("CrossIndustryOrderResponse")) {
            return new OrderResponseReader();
        }
        throw new CIIReaderException("Unable to detect message type from XML content");
    }
}
