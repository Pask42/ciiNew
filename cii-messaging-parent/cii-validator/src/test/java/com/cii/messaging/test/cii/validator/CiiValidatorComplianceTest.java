package com.cii.messaging.test.cii.validator;

import com.cii.messaging.test.cii.support.CiiSampleResource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Plan directeur pour les futurs scénarios de validation combinant XSD et Schematron.
 * La structure met en avant les axes clés (respect du schéma, règles métier,
 * restitution des erreurs) sans encore les faire appliquer.
 */
@DisplayName("CII Validator – vérifications de conformité")
class CiiValidatorComplianceTest {

    @Nested
    @DisplayName("Validation de schéma")
    class SchemaValidation {

        @Test
        @Disabled("Tests de validation XSD ajoutés ultérieurement")
        void shouldValidateInvoiceSampleAgainstXsd() {
            var samples = new CiiSampleResource();
            // Emplacement prévu pour la logique de validation XSD avec invoice-sample.xml
        }
    }

    @Nested
    @DisplayName("Règles métier Schematron")
    class SchematronValidation {

        @Test
        @Disabled("Tests de validation Schematron ajoutés ultérieurement")
        void shouldValidateOrderSampleAgainstSchematron() {
            var samples = new CiiSampleResource();
            // Emplacement prévu pour la logique de validation Schematron avec order-sample.xml
        }
    }

    @Test
    @Disabled("Tests de restitution d'erreurs ajoutés ultérieurement")
    @DisplayName("Collecte les échecs de validation avec détails de diagnostic")
    void shouldExposeReadableValidationErrors() {
        // Emplacement prévu pour vérifier la restitution structurée des erreurs du validateur
    }
}
