package com.cii.messaging.validator;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Supported CII message types for validation.
 */
public enum MessageType {
    INVOICE("CrossIndustryInvoice", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"),
    DESPATCH_ADVICE("CrossIndustryDespatchAdvice", "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:100"),
    ORDER("CrossIndustryOrder", "urn:un:unece:uncefact:data:standard:CrossIndustryOrder:100"),
    ORDER_RESPONSE("CrossIndustryOrderResponse", "urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:100");

    private static final Map<String, MessageType> BY_ROOT_ELEMENT = Map.of(
            INVOICE.rootElement, INVOICE,
            DESPATCH_ADVICE.rootElement, DESPATCH_ADVICE,
            ORDER.rootElement, ORDER,
            ORDER_RESPONSE.rootElement, ORDER_RESPONSE
    );

    private final String rootElement;
    private final Set<String> supportedNamespaces;

    MessageType(String rootElement, String namespace) {
        this.rootElement = Objects.requireNonNull(rootElement, "rootElement");
        this.supportedNamespaces = Set.of(Objects.requireNonNull(namespace, "namespace").trim());
    }

    public String getRootElement() {
        return rootElement;
    }

    public boolean supportsNamespace(String namespaceUri) {
        String candidate = namespaceUri == null ? "" : namespaceUri.trim();
        return supportedNamespaces.contains(candidate);
    }

    public String describeSupportedNamespaces() {
        return String.join(", ", supportedNamespaces);
    }

    public static MessageType fromRootElement(String rootElement) {
        return fromRootElement(rootElement, null);
    }

    public static MessageType fromRootElement(String rootElement, String namespaceUri) {
        Objects.requireNonNull(rootElement, "rootElement");
        MessageType messageType = BY_ROOT_ELEMENT.get(rootElement);
        if (messageType == null) {
            throw new IllegalArgumentException("Type de message non pris en charge : " + rootElement);
        }

        String sanitizedNamespace = namespaceUri == null ? "" : namespaceUri.trim();
        if (!sanitizedNamespace.isEmpty() && !messageType.supportsNamespace(sanitizedNamespace)) {
            throw new IllegalArgumentException(String.format(
                    "Espace de noms inattendu pour %s : %s (attendu %s)",
                    rootElement,
                    sanitizedNamespace,
                    messageType.describeSupportedNamespaces()));
        }
        return messageType;
    }
}
