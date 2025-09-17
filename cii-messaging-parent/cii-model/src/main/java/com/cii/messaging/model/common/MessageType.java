package com.cii.messaging.model.common;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Unified list of supported UN/CEFACT Cross Industry message types.
 * <p>
 * Centralising this enumeration avoids the numerous local duplicates that were
 * previously spread across the reader, writer and validator modules and keeps
 * the root element mapping in a single authoritative location.
 * </p>
 */
public enum MessageType {
    ORDER("CrossIndustryOrder"),
    ORDER_RESPONSE("CrossIndustryOrderResponse"),
    DESPATCH_ADVICE("CrossIndustryDespatchAdvice"),
    INVOICE("CrossIndustryInvoice");

    private final String rootElement;

    MessageType(String rootElement) {
        this.rootElement = rootElement;
    }

    /**
     * Returns the XML root element local name associated with the message type.
     */
    public String getRootElement() {
        return rootElement;
    }

    /**
     * Resolves a message type based on the provided XML root element.
     *
     * @param rootElement XML root element local name
     * @return the matching {@link MessageType}
     * @throws IllegalArgumentException if the element is not supported
     */
    public static MessageType fromRootElement(String rootElement) {
        Objects.requireNonNull(rootElement, "rootElement");
        return Arrays.stream(values())
                .filter(type -> type.rootElement.equals(rootElement))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Type de message non pris en charge : " + rootElement));
    }

    /**
     * Returns a human-readable label.
     */
    @Override
    public String toString() {
        return name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
