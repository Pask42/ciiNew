package com.cii.messaging.reader;

import com.cii.messaging.model.common.MessageType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Factory responsible for instantiating the appropriate {@link CIIReader}
 * implementation depending on either the expected {@link MessageType} or the
 * root element of the provided XML content.
 */
public final class CIIReaderFactory {

    private CIIReaderFactory() {
        // Utility class
    }

    public static CIIReader<?> createReader(MessageType messageType) {
        return switch (messageType) {
            case ORDER -> new OrderReader();
            case INVOICE -> new InvoiceReader();
            case DESPATCH_ADVICE -> new DesadvReader();
            case ORDER_RESPONSE -> new OrderResponseReader();
        };
    }

    public static CIIReader<?> createReader(String xmlContent) throws CIIReaderException {
        XMLInputFactory factory = createSecureXmlInputFactory();
        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(new StringReader(xmlContent));
            MessageType messageType = detectMessageType(reader);
            return createReader(messageType);
        } catch (XMLStreamException e) {
            throw new CIIReaderException("Contenu XML invalide", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ignored) {
                    // ignore
                }
            }
        }
    }

    public static CIIReader<?> createReader(Path xmlFile) throws CIIReaderException {
        XMLInputFactory factory = createSecureXmlInputFactory();
        XMLStreamReader reader = null;
        try (InputStream inputStream = Files.newInputStream(xmlFile)) {
            reader = factory.createXMLStreamReader(inputStream);
            MessageType messageType = detectMessageType(reader);
            return createReader(messageType);
        } catch (IOException | XMLStreamException e) {
            throw new CIIReaderException("Fichier XML invalide", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ignored) {
                    // ignore
                }
            }
        }
    }

    private static XMLInputFactory createSecureXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        return factory;
    }

    private static MessageType detectMessageType(XMLStreamReader reader) throws XMLStreamException, CIIReaderException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) {
                throw new CIIReaderException("DOCTYPE non autorisé", new XMLStreamException("DOCTYPE non autorisé"));
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                try {
                    return MessageType.fromRootElement(localName);
                } catch (IllegalArgumentException ex) {
                    throw new CIIReaderException("Type de message non pris en charge : " + localName, ex);
                }
            }
        }
        throw new CIIReaderException("Impossible de détecter le type de message à partir du contenu XML");
    }
}
