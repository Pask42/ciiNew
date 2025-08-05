package com.cii.messaging.reader;

import com.cii.messaging.model.CIIMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Utility for converting XML order documents into {@link CIIMessage} instances.
 */
public class JaxbOrderReader implements CIIReader {

    private final JAXBContext context;

    public JaxbOrderReader() {
        try {
            this.context = JAXBContext.newInstance(CIIMessage.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to initialize JAXB context", e);
        }
    }

    @Override
    public CIIMessage read(File xmlFile) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (CIIMessage) unmarshaller.unmarshal(xmlFile);
        } catch (JAXBException e) {
            throw new CIIReaderException("Failed to parse XML file: " + xmlFile.getName(), e);
        }
    }

    @Override
    public CIIMessage read(InputStream inputStream) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (CIIMessage) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new CIIReaderException("Failed to parse XML from input stream", e);
        }
    }

    @Override
    public CIIMessage read(String xml) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (CIIMessage) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new CIIReaderException("Failed to parse XML content", e);
        }
    }
}
