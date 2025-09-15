package com.cii.messaging.reader;

import com.cii.messaging.model.invoice.Invoice;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;

/**
 * JAXB based reader for CrossIndustryInvoice documents.
 */
public class InvoiceReader implements CIIReader<Invoice> {

    private final JAXBContext context;

    public InvoiceReader() {
        try {
            this.context = JAXBContext.newInstance(Invoice.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Échec de l'initialisation du contexte JAXB", e);
        }
    }

    @Override
    public Invoice read(File xmlFile) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Invoice) unmarshaller.unmarshal(xmlFile);
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du fichier XML : " + xmlFile.getName(), e);
        }
    }

    @Override
    public Invoice read(InputStream inputStream) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Invoice) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du XML depuis le flux d'entrée", e);
        }
    }

    @Override
    public Invoice read(String xmlContent) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Invoice) unmarshaller.unmarshal(new StringReader(xmlContent));
        } catch (JAXBException e) {
            throw new CIIReaderException("Échec de l'analyse du contenu XML", e);
        }
    }
}
