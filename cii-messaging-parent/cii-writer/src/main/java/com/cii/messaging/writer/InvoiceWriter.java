package com.cii.messaging.writer;

import com.cii.messaging.model.CIIMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;

/**
 * Serializes {@link CIIMessage} instances into XML invoices.
 */
public class InvoiceWriter {

    private final JAXBContext context;

    public InvoiceWriter() throws JAXBException {
        this.context = JAXBContext.newInstance(CIIMessage.class);
    }

    /**
     * Marshals the provided message into an XML document.
     *
     * @param message message to marshal
     * @return XML representation of the message
     * @throws JAXBException if marshalling fails
     */
    public String write(CIIMessage message) throws JAXBException {
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(message, writer);
        return writer.toString();
    }
}
