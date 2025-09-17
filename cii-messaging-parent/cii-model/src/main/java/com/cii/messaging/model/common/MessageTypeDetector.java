package com.cii.messaging.model.common;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.cii.messaging.model.common.MessageTypeDetectionException.Reason.EMPTY_DOCUMENT;
import static com.cii.messaging.model.common.MessageTypeDetectionException.Reason.INVALID_XML;
import static com.cii.messaging.model.common.MessageTypeDetectionException.Reason.PROHIBITED_DTD;

/**
 * Utility class providing secure detection of message types directly from XML payloads.
 */
public final class MessageTypeDetector {

    private MessageTypeDetector() {
        // Utility class
    }

    /**
     * Creates a new {@link XMLInputFactory} configured to forbid DTDs and external entities.
     */
    public static XMLInputFactory newXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        return factory;
    }

    /**
     * Detects the {@link MessageType} from the provided XML file.
     */
    public static MessageType detect(Path xmlFile) throws IOException, MessageTypeDetectionException {
        Objects.requireNonNull(xmlFile, "xmlFile");
        try (InputStream inputStream = Files.newInputStream(xmlFile)) {
            return detect(inputStream);
        }
    }

    /**
     * Detects the {@link MessageType} from the provided XML string content.
     */
    public static MessageType detect(String xmlContent) throws MessageTypeDetectionException {
        Objects.requireNonNull(xmlContent, "xmlContent");
        if (xmlContent.isBlank()) {
            throw new MessageTypeDetectionException(
                    "Document XML vide : type de message introuvable",
                    MessageTypeDetectionException.Reason.EMPTY_DOCUMENT);
        }
        XMLInputFactory factory = newXmlInputFactory();
        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(new StringReader(xmlContent));
            return detect(reader);
        } catch (XMLStreamException e) {
            throw wrapXmlException("Échec de l'analyse du contenu XML", e);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     * Detects the {@link MessageType} from the provided XML byte array.
     */
    public static MessageType detect(byte[] xmlContent) throws MessageTypeDetectionException {
        Objects.requireNonNull(xmlContent, "xmlContent");
        if (xmlContent.length == 0) {
            throw new MessageTypeDetectionException(
                    "Document XML vide : type de message introuvable",
                    MessageTypeDetectionException.Reason.EMPTY_DOCUMENT);
        }
        XMLInputFactory factory = newXmlInputFactory();
        XMLStreamReader reader = null;
        try (ByteArrayInputStream input = new ByteArrayInputStream(xmlContent)) {
            reader = factory.createXMLStreamReader(input);
            return detect(reader);
        } catch (XMLStreamException e) {
            throw wrapXmlException("Échec de la lecture du flux XML", e);
        } catch (IOException e) {
            // ByteArrayInputStream#close ne déclenche jamais d'IOException mais on reste complet.
            throw new MessageTypeDetectionException("Échec de la lecture du flux XML", e, INVALID_XML);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     * Detects the {@link MessageType} from the provided {@link InputStream}.
     */
    public static MessageType detect(InputStream xmlContent) throws MessageTypeDetectionException {
        Objects.requireNonNull(xmlContent, "xmlContent");
        XMLInputFactory factory = newXmlInputFactory();
        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(xmlContent);
            return detect(reader);
        } catch (XMLStreamException e) {
            throw wrapXmlException("Échec de la lecture du flux XML", e);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     * Detects the {@link MessageType} using an existing {@link XMLStreamReader}.
     */
    public static MessageType detect(XMLStreamReader reader) throws MessageTypeDetectionException {
        Objects.requireNonNull(reader, "reader");
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) {
                    throw new MessageTypeDetectionException("DOCTYPE non autorisé", PROHIBITED_DTD);
                }
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    String namespace = reader.getNamespaceURI();
                    try {
                        return MessageType.fromRootElement(localName, namespace);
                    } catch (IllegalArgumentException ex) {
                        throw new MessageTypeDetectionException(ex.getMessage(), ex,
                                MessageTypeDetectionException.Reason.UNKNOWN_ROOT);
                    }
                }
            }
            throw new MessageTypeDetectionException(
                    "Document XML vide : type de message introuvable",
                    EMPTY_DOCUMENT);
        } catch (XMLStreamException e) {
            if (e instanceof MessageTypeDetectionException detectionException) {
                throw detectionException;
            }
            throw new MessageTypeDetectionException("Échec de l'analyse du contenu XML", e, INVALID_XML);
        }
    }

    private static MessageTypeDetectionException wrapXmlException(String message, XMLStreamException e)
            throws MessageTypeDetectionException {
        if (e instanceof MessageTypeDetectionException detectionException) {
            throw detectionException;
        }
        throw new MessageTypeDetectionException(message, e, INVALID_XML);
    }

    private static void closeQuietly(XMLStreamReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException ignored) {
                // Ignored on purpose
            }
        }
    }
}
