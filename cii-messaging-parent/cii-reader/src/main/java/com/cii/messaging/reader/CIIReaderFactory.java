package com.cii.messaging.reader;

import com.cii.messaging.model.common.MessageType;
import com.cii.messaging.model.common.MessageTypeDetectionException;
import com.cii.messaging.model.common.MessageTypeDetector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

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
        Objects.requireNonNull(messageType, "messageType");
        return switch (messageType) {
            case ORDER -> new OrderReader();
            case INVOICE -> new InvoiceReader();
            case DESPATCH_ADVICE -> new DesadvReader();
            case ORDER_RESPONSE -> new OrderResponseReader();
        };
    }

    public static CIIReader<?> createReader(String xmlContent) throws CIIReaderException {
        try {
            MessageType messageType = MessageTypeDetector.detect(xmlContent);
            return createReader(messageType);
        } catch (MessageTypeDetectionException e) {
            throw mapDetectionException(e);
        }
    }

    public static CIIReader<?> createReader(Path xmlFile) throws CIIReaderException {
        try (InputStream inputStream = Files.newInputStream(xmlFile)) {
            MessageType messageType = MessageTypeDetector.detect(inputStream);
            return createReader(messageType);
        } catch (IOException e) {
            throw new CIIReaderException("Impossible de lire le fichier XML", e);
        } catch (MessageTypeDetectionException e) {
            throw mapDetectionException(e);
        }
    }

    private static CIIReaderException mapDetectionException(MessageTypeDetectionException e) {
        return switch (e.getReason()) {
            case PROHIBITED_DTD -> new CIIReaderException("DOCTYPE non autorisé", e);
            case UNKNOWN_ROOT -> new CIIReaderException(e.getMessage(), e);
            case EMPTY_DOCUMENT ->
                    new CIIReaderException("Impossible de détecter le type de message à partir du contenu XML", e);
            case INVALID_XML -> new CIIReaderException("Contenu XML invalide", e);
        };
    }
}
