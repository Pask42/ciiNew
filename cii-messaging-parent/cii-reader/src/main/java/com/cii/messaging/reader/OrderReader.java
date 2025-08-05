package com.cii.messaging.reader;

import com.cii.messaging.model.CIIMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;

/**
 * Utility for converting XML order documents into {@link CIIMessage} instances.
 */
public class OrderReader {

    private final JAXBContext context;

    public OrderReader() throws JAXBException {
        this.context = JAXBContext.newInstance(CIIMessage.class);
    }

    /**
     * Parses the provided XML into a {@link CIIMessage}.
     *
     * @param xml XML string representing the CII message
     * @return parsed {@link CIIMessage}
     * @throws JAXBException if parsing fails
     */
    public CIIMessage read(String xml) throws JAXBException {
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (CIIMessage) unmarshaller.unmarshal(new StringReader(xml));
    }
}
