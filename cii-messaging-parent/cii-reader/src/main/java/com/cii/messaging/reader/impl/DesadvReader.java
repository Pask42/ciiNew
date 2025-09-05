package com.cii.messaging.reader.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.reader.CIIReaderException;
import jakarta.xml.bind.JAXBException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class DesadvReader extends AbstractCIIReader {
    
    @Override
    protected void initializeJAXBContext() throws JAXBException {
        // Simple XML parsing without specific JAXB context
    }
    
    @Override
    public CIIMessage read(File xmlFile) throws CIIReaderException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            return parseDocument(doc);
        } catch (Exception e) {
            throw new CIIReaderException("Échec de la lecture du fichier DESADV", e);
        }
    }
    
    @Override
    protected CIIMessage parseDocument(Object document) throws CIIReaderException {
        // Basic implementation for DESADV parsing
        return CIIMessage.builder()
                  .messageType(MessageType.DESADV)
                  .creationDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                  .build();
    }
}
