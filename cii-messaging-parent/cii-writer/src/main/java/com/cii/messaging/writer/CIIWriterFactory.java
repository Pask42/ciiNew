package com.cii.messaging.writer;

import com.cii.messaging.model.MessageType;
import com.cii.messaging.writer.impl.*;

public class CIIWriterFactory {
    
    public static CIIWriter createWriter(MessageType messageType) {
        switch (messageType) {
            case INVOICE:
                return new InvoiceWriter();
            case DESADV:
                return new DesadvWriter();
            case ORDER:
                return new OrderWriter();
            case ORDERSP:
                return new OrderResponseWriter();
            default:
                throw new IllegalArgumentException("Unsupported message type for writing: " + messageType);
        }
    }
    
    public static CIIWriter createWriter(MessageType messageType, WriterConfig config) {
        CIIWriter writer = createWriter(messageType);
        if (config != null) {
            writer.setFormatOutput(config.isFormatOutput());
            writer.setEncoding(config.getEncoding());
        }
        return writer;
    }
}
