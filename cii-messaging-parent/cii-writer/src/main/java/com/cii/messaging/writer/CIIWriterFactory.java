package com.cii.messaging.writer;

import com.cii.messaging.model.common.MessageType;

/**
 * Factory providing writer implementations based on {@link MessageType}.
 */
public class CIIWriterFactory {

    @SuppressWarnings("unchecked")
    public static <T> CIIWriter<T> createWriter(MessageType messageType) {
        return switch (messageType) {
            case INVOICE -> (CIIWriter<T>) new InvoiceWriter();
            case DESPATCH_ADVICE -> (CIIWriter<T>) new DesadvWriter();
            case ORDER -> (CIIWriter<T>) new OrderWriter();
            case ORDER_RESPONSE -> (CIIWriter<T>) new OrderResponseWriter();
        };
    }

    public static <T> CIIWriter<T> createWriter(MessageType messageType, WriterConfig config) {
        CIIWriter<T> writer = createWriter(messageType);
        if (config != null) {
            writer.setFormatOutput(config.isFormatOutput());
            writer.setEncoding(config.getEncoding());
        }
        return writer;
    }
}
