package com.cii.messaging.writer;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;

/**
 * Implémentation générique de {@link CIIWriter} reposant sur JAXB.
 */
public class JaxbWriter<T> implements CIIWriter<T> {
    private final JAXBContext context;
    private boolean formatOutput = true;
    private String encoding = "UTF-8";

    public JaxbWriter(Class<T> type) {
        try {
            this.context = JAXBContext.newInstance(type);
        } catch (JAXBException e) {
            throw new IllegalStateException("Impossible de créer le JAXBContext", e);
        }
    }

    @Override
    public void write(T message, File outputFile) throws CIIWriterException {
        try (OutputStream os = new java.io.FileOutputStream(outputFile)) {
            write(message, os);
        } catch (Exception e) {
            throw new CIIWriterException("Échec de l'écriture du message", e);
        }
    }

    @Override
    public void write(T message, OutputStream outputStream) throws CIIWriterException {
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatOutput);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
            marshaller.marshal(message, outputStream);
        } catch (JAXBException e) {
            throw new CIIWriterException("Échec de l'écriture du message", e);
        }
    }

    @Override
    public String writeToString(T message) throws CIIWriterException {
        try {
            StringWriter sw = new StringWriter();
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatOutput);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
            marshaller.marshal(message, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new CIIWriterException("Échec de l'écriture du message", e);
        }
    }

    @Override
    public void setFormatOutput(boolean format) {
        this.formatOutput = format;
    }

    @Override
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
