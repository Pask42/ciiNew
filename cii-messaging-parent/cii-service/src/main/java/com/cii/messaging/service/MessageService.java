package com.cii.messaging.service;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.validator.MessageValidator;
import com.cii.messaging.writer.InvoiceWriter;

/**
 * Facade for generating, parsing and validating CII messages.
 */
public class MessageService {

    private final CIIReader reader;
    private final InvoiceWriter writer;
    private final MessageValidator validator;

    public MessageService(CIIReader reader, InvoiceWriter writer, MessageValidator validator) {
        this.reader = reader;
        this.writer = writer;
        this.validator = validator;
    }

    /**
     * Generates an XML representation of the supplied message.
     *
     * @param message message to serialize
     * @return XML string
     * @throws Exception if serialization or validation fails
     */
    public String generate(CIIMessage message) throws Exception {
        validator.validate(message);
        return writer.write(message);
    }

    /**
     * Parses the provided XML into a {@link CIIMessage}.
     *
     * @param xml message in XML form
     * @return parsed message
     * @throws Exception if parsing or validation fails
     */
    public CIIMessage parse(String xml) throws Exception {
        CIIMessage message = reader.read(xml);
        validator.validate(message);
        return message;
    }

    /**
     * Validates the supplied message.
     *
     * @param message message to validate
     * @return {@code true} if validation succeeds
     * @throws Exception if validation fails
     */
    public boolean validate(CIIMessage message) throws Exception {
        validator.validate(message);
        return true;
    }
}
