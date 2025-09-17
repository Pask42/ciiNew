package com.cii.messaging.model.common;

import com.cii.messaging.model.despatchadvice.DespatchAdvice;
import com.cii.messaging.model.invoice.Invoice;
import com.cii.messaging.model.order.Order;
import com.cii.messaging.model.orderresponse.OrderResponse;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Unified list of supported UN/CEFACT Cross Industry message types.
 * <p>
 * Each type exposes metadata that is shared across the reader, writer and
 * validator modules so that the mapping stays consistent and centralised.
 * </p>
 */
public enum MessageType {
    ORDER("CrossIndustryOrder", "urn:un:unece:uncefact:data:standard:CrossIndustryOrder:16", Order.class),
    ORDER_RESPONSE("CrossIndustryOrderResponse", "urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:100", OrderResponse.class),
    DESPATCH_ADVICE("CrossIndustryDespatchAdvice", "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:100", DespatchAdvice.class),
    INVOICE("CrossIndustryInvoice", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100", Invoice.class);

    private final String rootElement;
    private final String namespace;
    private final Class<?> modelClass;
    private final String displayName;

    MessageType(String rootElement, String namespace, Class<?> modelClass) {
        this.rootElement = rootElement;
        this.namespace = namespace;
        this.modelClass = modelClass;
        this.displayName = buildDisplayName(name());
    }

    /**
     * Returns the XML root element local name associated with the message type.
     */
    public String getRootElement() {
        return rootElement;
    }

    /**
     * Returns the XML namespace URI associated with the message type.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the JAXB model class representing this message type.
     */
    public Class<?> getModelClass() {
        return modelClass;
    }

    /**
     * Returns a descriptive label for display purposes.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the qualified name of the XML root element for the message type.
     */
    public QName getRootQName() {
        return new QName(namespace, rootElement);
    }

    /**
     * Resolves a message type based on the provided XML root element.
     *
     * @param rootElement XML root element local name
     * @return the matching {@link MessageType}
     * @throws IllegalArgumentException if the element is not supported
     */
    public static MessageType fromRootElement(String rootElement) {
        return fromRootElement(rootElement, null);
    }

    /**
     * Resolves a message type based on the provided XML root element and namespace.
     *
     * @param rootElement XML root element local name
     * @param namespace   XML namespace URI of the root element
     * @return the matching {@link MessageType}
     * @throws IllegalArgumentException if the element is not supported
     */
    public static MessageType fromRootElement(String rootElement, String namespace) {
        Objects.requireNonNull(rootElement, "rootElement");
        return Arrays.stream(values())
                .filter(type -> type.rootElement.equals(rootElement)
                        && (namespace == null || namespace.isBlank() || type.namespace.equals(namespace)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(buildUnsupportedMessage(rootElement, namespace)));
    }

    /**
     * Resolves a message type based on the JAXB model class.
     *
     * @param modelClass JAXB model class
     * @return the matching {@link MessageType}
     * @throws IllegalArgumentException if no type matches the class
     */
    public static MessageType fromModelClass(Class<?> modelClass) {
        Objects.requireNonNull(modelClass, "modelClass");
        return Arrays.stream(values())
                .filter(type -> type.modelClass.isAssignableFrom(modelClass) || modelClass.isAssignableFrom(type.modelClass))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Type de modèle non pris en charge : " + modelClass.getName()));
    }

    private static String buildUnsupportedMessage(String rootElement, String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return "Type de message non pris en charge : " + rootElement;
        }
        return "Type de message non pris en charge : " + rootElement + " (namespace: " + namespace + ")";
    }

    private static String buildDisplayName(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    /**
     * Returns a human-readable label.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
