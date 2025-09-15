package com.cii.messaging.reader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Generic JAXB-based implementation of {@link CIIReader}.
 *
 * @param <T> message model type
 */
public abstract class JaxbReader<T> implements CIIReader<T> {

    private final Class<T> type;
    private final JAXBContext context;

    protected JaxbReader(Class<T> type) {
        this.type = type;
        try {
            this.context = JAXBContext.newInstance(type);
        } catch (JAXBException e) {
            throw new IllegalStateException("Échec de l'initialisation du contexte JAXB", e);
        }
    }

    @Override
    public T read(File xmlFile) throws CIIReaderException {
        try (InputStream inputStream = Files.newInputStream(xmlFile.toPath())) {
            byte[] data = inputStream.readAllBytes();
            return readFromByteArray(data, "Échec de l'analyse du fichier XML : " + xmlFile.getName());
        } catch (IOException e) {
            throw new CIIReaderException("Échec de l'analyse du fichier XML : " + xmlFile.getName(), e);
        }
    }

    @Override
    public T read(InputStream inputStream) throws CIIReaderException {
        try {
            byte[] data = inputStream.readAllBytes();
            return readFromByteArray(data, "Échec de l'analyse du XML depuis le flux d'entrée");
        } catch (IOException e) {
            throw new CIIReaderException("Échec de l'analyse du XML depuis le flux d'entrée", e);
        }
    }

    @Override
    public T read(String xmlContent) throws CIIReaderException {
        byte[] data = xmlContent.getBytes(StandardCharsets.UTF_8);
        return readFromByteArray(data, "Échec de l'analyse du contenu XML");
    }

    private T readFromByteArray(byte[] data, String errorMessage) throws CIIReaderException {
        XMLInputFactory factory = createSecureXmlInputFactory();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return unmarshal(unmarshaller -> {
            XMLStreamReader xmlReader = factory.createXMLStreamReader(inputStream);
            try {
                return unmarshaller.unmarshal(xmlReader);
            } finally {
                xmlReader.close();
            }
        }, errorMessage);
    }

    private T unmarshal(UnmarshalOperation operation, String errorMessage) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object result = operation.unmarshal(unmarshaller);
            return type.cast(result);
        } catch (ClassCastException e) {
            throw new CIIReaderException("Le contenu XML ne correspond pas au type attendu " + type.getSimpleName(), e);
        } catch (JAXBException e) {
            throw new CIIReaderException(errorMessage, e);
        } catch (Exception e) {
            throw new CIIReaderException(errorMessage, e);
        }
    }

    private static XMLInputFactory createSecureXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        return factory;
    }

    @FunctionalInterface
    private interface UnmarshalOperation {
        Object unmarshal(Unmarshaller unmarshaller) throws Exception;
    }
}
