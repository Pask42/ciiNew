package com.cii.messaging.writer.generation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentStatusCodesTest {

    @Test
    void chargeCodesDepuisLeSchemaD23B() {
        Set<String> codes = DocumentStatusCodes.validCodes();

        assertFalse(codes.isEmpty(), "La liste des codes UNECE ne doit pas être vide");
        assertTrue(codes.contains(DocumentStatusCodes.DEFAULT_ACKNOWLEDGEMENT_CODE),
                "Le code par défaut doit être présent dans la liste");
        assertTrue(codes.contains("1"), "Le premier code de la nomenclature doit être disponible");
        assertTrue(codes.contains("51"), "Le dernier code de la nomenclature D23B doit être disponible");
        assertFalse(codes.contains("0"), "Les valeurs hors nomenclature doivent être rejetées");
    }
}
