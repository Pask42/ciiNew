package com.cii.messaging.reader.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.reader.CIIReaderException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public abstract class AbstractCIIReader implements CIIReader {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractCIIReader.class);
    protected JAXBContext jaxbContext;
    
    protected AbstractCIIReader() {
        try {
            initializeJAXBContext();
        } catch (JAXBException e) {
            throw new RuntimeException("Échec de l'initialisation du contexte JAXB", e);
        }
    }
    
    protected abstract void initializeJAXBContext() throws JAXBException;
    protected abstract CIIMessage parseDocument(Object document) throws CIIReaderException;
    
    @Override
    public CIIMessage read(File xmlFile) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object document = unmarshaller.unmarshal(xmlFile);
            return parseDocument(document);
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du fichier XML : " + xmlFile.getName(), e);
        }
    }
    
    @Override
    public CIIMessage read(InputStream inputStream) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object document = unmarshaller.unmarshal(inputStream);
            return parseDocument(document);
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du XML depuis le flux d'entrée", e);
        }
    }
    
    @Override
    public CIIMessage read(String xmlContent) throws CIIReaderException {
        return read(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }
    
    protected BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            logger.warn("Impossible d'analyser BigDecimal depuis : {}", value);
            return null;
        }
    }
}
