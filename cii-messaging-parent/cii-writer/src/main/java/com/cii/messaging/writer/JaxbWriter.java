package com.cii.messaging.writer;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;

/**
 * Implémentation générique de {@link CIIWriter} reposant sur JAXB.
 */
public class JaxbWriter<T> implements CIIWriter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JaxbWriter.class);
    private static final String NAMESPACE_PREFIX_MAPPER_PROPERTY = "com.sun.xml.bind.namespacePrefixMapper";

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
            Marshaller marshaller = createMarshaller();
            marshaller.marshal(message, outputStream);
        } catch (JAXBException e) {
            throw new CIIWriterException("Échec de l'écriture du message", e);
        }
    }

    @Override
    public String writeToString(T message) throws CIIWriterException {
        try {
            StringWriter sw = new StringWriter();
            Marshaller marshaller = createMarshaller();
            marshaller.marshal(message, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new CIIWriterException("Échec de l'écriture du message", e);
        }
    }

    private Marshaller createMarshaller() throws JAXBException {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatOutput);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
        configureNamespacePrefixes(marshaller);
        return marshaller;
    }

    private void configureNamespacePrefixes(Marshaller marshaller) {
        CIINamespacePrefixMapper mapper = new CIINamespacePrefixMapper();
        if (!setNamespaceMapperProperty(marshaller, NAMESPACE_PREFIX_MAPPER_PROPERTY, mapper)) {
            setNamespaceMapperProperty(marshaller, "org.glassfish.jaxb.namespacePrefixMapper", mapper);
        }
    }

    private boolean setNamespaceMapperProperty(Marshaller marshaller, String property, CIINamespacePrefixMapper mapper) {
        try {
            marshaller.setProperty(property, mapper);
            return true;
        } catch (PropertyException ex) {
            LOGGER.debug("Namespace prefix mapper property '{}' not supported by current JAXB implementation", property, ex);
            return false;
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
