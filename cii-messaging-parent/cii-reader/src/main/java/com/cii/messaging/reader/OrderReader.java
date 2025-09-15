package com.cii.messaging.reader;

import com.cii.messaging.model.order.Order;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;

/**
 * JAXB based reader for CrossIndustryOrder documents.
 */
public class OrderReader implements CIIReader<Order> {

    private final JAXBContext context;

    public OrderReader() {
        try {
            this.context = JAXBContext.newInstance(Order.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Échec de l'initialisation du contexte JAXB", e);
        }
    }

    @Override
    public Order read(File xmlFile) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Order) unmarshaller.unmarshal(xmlFile);
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du fichier XML : " + xmlFile.getName(), e);
        }
    }

    @Override
    public Order read(InputStream inputStream) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Order) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du XML depuis le flux d'entrée", e);
        }
    }

    @Override
    public Order read(String xmlContent) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Order) unmarshaller.unmarshal(new StringReader(xmlContent));
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du contenu XML", e);
        }
    }
}
