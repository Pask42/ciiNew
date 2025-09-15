package com.cii.messaging.reader;

import com.cii.messaging.model.orderresponse.OrderResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;

/**
 * JAXB based reader for CrossIndustryOrderResponse documents.
 */
public class OrderResponseReader implements CIIReader<OrderResponse> {

    private final JAXBContext context;

    public OrderResponseReader() {
        try {
            this.context = JAXBContext.newInstance(OrderResponse.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Échec de l'initialisation du contexte JAXB", e);
        }
    }

    @Override
    public OrderResponse read(File xmlFile) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (OrderResponse) unmarshaller.unmarshal(xmlFile);
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du fichier XML : " + xmlFile.getName(), e);
        }
    }

    @Override
    public OrderResponse read(InputStream inputStream) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (OrderResponse) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du XML depuis le flux d'entrée", e);
        }
    }

    @Override
    public OrderResponse read(String xmlContent) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (OrderResponse) unmarshaller.unmarshal(new StringReader(xmlContent));
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du contenu XML", e);
        }
    }
}
