package com.cii.messaging.writer.generation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderResponseGenerationOptionsTest {

    @Test
    void utiliseCodeParDefautConforme() {
        OrderResponseGenerationOptions options = OrderResponseGenerationOptions.defaults();

        assertEquals(AcknowledgementCodes.DEFAULT_ACKNOWLEDGEMENT_CODE, options.getAcknowledgementCode());
    }

    @Test
    void rejetteCodeNonReferenceParUnece() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                OrderResponseGenerationOptions.builder()
                        .withAcknowledgementCode("AP")
                        .build());

        assertEquals("Code d'accusé de réception 'AP' invalide. Référez-vous à la liste UNECE : "
                        + AcknowledgementCodes.validCodes(), exception.getMessage());
    }

    @Test
    void accepteUnLineStatusCodeValide() {
        OrderResponseGenerationOptions options = OrderResponseGenerationOptions.builder()
                .withLineStatusCode("5")
                .build();

        assertEquals("5", options.getLineStatusCode());
    }

    @Test
    void rejetteUnLineStatusCodeInvalide() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                OrderResponseGenerationOptions.builder()
                        .withLineStatusCode("999")
                        .build());

        assertEquals("Code de statut de ligne '999' invalide. Référez-vous à la liste UNECE : "
                        + LineStatusCodes.validCodes(), exception.getMessage());
    }
}
