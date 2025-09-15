package com.cii.messaging.validator;

import java.util.Arrays;

/**
 * Supported CII message types for validation.
 */
public enum MessageType {
    INVOICE("CrossIndustryInvoice"),
    DESPATCH_ADVICE("CrossIndustryDespatchAdvice"),
    ORDER("CrossIndustryOrder"),
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
