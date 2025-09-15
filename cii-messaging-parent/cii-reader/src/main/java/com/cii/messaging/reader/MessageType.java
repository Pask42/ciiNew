package com.cii.messaging.reader;

import java.util.Arrays;

/**
 * Supported Cross Industry document types with their associated XML root elements.
 */
public enum MessageType {
    ORDER("CrossIndustryOrder"),
    INVOICE("CrossIndustryInvoice"),
    DESPATCH_ADVICE("CrossIndustryDespatchAdvice"),
    ORDER_RESPONSE("CrossIndustryOrderResponse");

    private final String rootElement;

    MessageType(String rootElement) {
        this.rootElement = rootElement;
    }

    public String getRootElement() {
        return rootElement;
    }

    public static MessageType fromRootElement(String rootElement) {
        return Arrays.stream(values())
                .filter(type -> type.rootElement.equals(rootElement))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Type de message non pris en charge : " + rootElement));
    }
}
