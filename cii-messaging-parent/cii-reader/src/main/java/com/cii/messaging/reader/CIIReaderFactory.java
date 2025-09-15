package com.cii.messaging.reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory responsible for instantiating the appropriate {@link CIIReader}
 * implementation depending on either the expected {@link MessageType} or the
 * root element of the provided XML content.
 */
public final class CIIReaderFactory {

    private static final Map<MessageType, CIIReader<?>> READERS;

    static {
        EnumMap<MessageType, CIIReader<?>> readers = new EnumMap<>(MessageType.class);
        readers.put(MessageType.ORDER, new OrderReader());
        readers.put(MessageType.INVOICE, new InvoiceReader());
        readers.put(MessageType.DESPATCH_ADVICE, new DesadvReader());
        readers.put(MessageType.ORDER_RESPONSE, new OrderResponseReader());
        READERS = Collections.unmodifiableMap(readers);
    }

    private CIIReaderFactory() {
        // Utility class
    }

    public static CIIReader<?> createReader(MessageType messageType) {
        Objects.requireNonNull(messageType, "messageType");
        CIIReader<?> reader = READERS.get(messageType);
        if (reader == null) {
            throw new IllegalArgumentException("Type de message non pris en charge : " + messageType);
        }
        return reader;
    }

    public static CIIReader<?> createReader(String xmlContent) throws CIIReaderException {
        Objects.requireNonNull(xmlContent, "xmlContent");
        XMLInputFactory factory = createSecureXmlInputFactory();
        try (StringReader contentReader = new StringReader(xmlContent)) {
            XMLStreamReader xmlReader = factory.createXMLStreamReader(contentReader);
            try {
                MessageType messageType = detectMessageType(xmlReader);
                return createReader(messageType);
            } finally {
                try {
                    xmlReader.close();
                } catch (XMLStreamException ignored) {
                    // ignore
                }
            }
        } catch (XMLStreamException e) {
            throw new CIIReaderException("Contenu XML invalide", e);
        }
    }

    public static CIIReader<?> createReader(Path xmlFile) throws CIIReaderException {
        Objects.requireNonNull(xmlFile, "xmlFile");
        XMLInputFactory factory = createSecureXmlInputFactory();
        try (InputStream inputStream = Files.newInputStream(xmlFile)) {
            XMLStreamReader xmlReader = factory.createXMLStreamReader(inputStream);
            try {
                MessageType messageType = detectMessageType(xmlReader);
                return createReader(messageType);
            } finally {
                try {
                    xmlReader.close();
                } catch (XMLStreamException ignored) {
                    // ignore
                }
            }
        } catch (IOException | XMLStreamException e) {
            throw new CIIReaderException("Fichier XML invalide", e);
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
                String namespace = reader.getNamespaceURI();
                try {
                    return MessageType.fromRootElement(localName, namespace);
                } catch (IllegalArgumentException ex) {
                    throw new CIIReaderException(ex.getMessage(), ex);
                }
            }
        }
        throw new CIIReaderException("Impossible de détecter le type de message à partir du contenu XML");
    }
}
