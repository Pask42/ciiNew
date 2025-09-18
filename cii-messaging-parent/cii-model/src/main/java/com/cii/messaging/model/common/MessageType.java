package com.cii.messaging.model.common;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Liste unifiée des types de messages UN/CEFACT Cross Industry pris en charge.
 * <p>
 * Centraliser cette énumération évite les doublons locaux auparavant répartis
 * entre les modules reader, writer et validator et conserve la correspondance
 * des éléments racine dans un point unique de référence.
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
     * Retourne le nom local de l'élément racine XML associé au type de message.
     */
    public String getRootElement() {
        return rootElement;
    }

    /**
     * Résout un type de message à partir de l'élément racine XML fourni.
     *
     * @param rootElement nom local de l'élément racine XML
     * @return le {@link MessageType} correspondant
     * @throws IllegalArgumentException si l'élément n'est pas pris en charge
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
     * Retourne un libellé lisible par un humain.
     */
    @Override
    public String toString() {
        return name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
