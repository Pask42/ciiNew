package com.cii.messaging.reader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;

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
        return unmarshal(unmarshaller -> unmarshaller.unmarshal(xmlFile),
                "Échec de l'analyse du fichier XML : " + xmlFile.getName());
    }

    @Override
    public T read(InputStream inputStream) throws CIIReaderException {
        return unmarshal(unmarshaller -> unmarshaller.unmarshal(inputStream),
                "Échec de l'analyse du XML depuis le flux d'entrée");
    }

    @Override
    public T read(String xmlContent) throws CIIReaderException {
        try (StringReader reader = new StringReader(xmlContent)) {
            return unmarshal(unmarshaller -> unmarshaller.unmarshal(reader),
                    "Échec de l'analyse du contenu XML");
        }
    }

    private T unmarshal(UnmarshalOperation operation, String errorMessage) throws CIIReaderException {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object result = operation.unmarshal(unmarshaller);
            return type.cast(result);
        } catch (JAXBException e) {
            throw new CIIReaderException(errorMessage, e);
        }
    }

    @FunctionalInterface
    private interface UnmarshalOperation {
        Object unmarshal(Unmarshaller unmarshaller) throws JAXBException;
    }
}
