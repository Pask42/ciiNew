package com.cii.messaging.model.common;

import javax.xml.stream.XMLStreamException;

/**
 * Exception raised when the message type of an XML payload cannot be determined safely.
 */
public class MessageTypeDetectionException extends XMLStreamException {

    /**
     * Enumerates the high level reasons why detection can fail.
     */
    public enum Reason {
        /** The XML document contains a prohibited DOCTYPE or external entity. */
        PROHIBITED_DTD,
        /** The XML is well-formed but the root element is unknown. */
        UNKNOWN_ROOT,
        /** The XML document does not contain any start element. */
        EMPTY_DOCUMENT,
        /** The XML is malformed or cannot be parsed. */
        INVALID_XML
    }

    private final Reason reason;

    public MessageTypeDetectionException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public MessageTypeDetectionException(String message, Throwable cause, Reason reason) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
