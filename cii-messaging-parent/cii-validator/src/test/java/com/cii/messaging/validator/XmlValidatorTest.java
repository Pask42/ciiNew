package com.cii.messaging.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class XmlValidatorTest {

    private static final Path RESSOURCES = Path.of("src", "test", "resources", "schema");

    @Test
    void validerFichierXML_retourneMessageSucces_quandXmlConforme() throws IOException {
        Path xmlPath = RESSOURCES.resolve("order-valid.xml");
        Path xsdPath = RESSOURCES.resolve("simple-order.xsd");

        String message = XmlValidator.validerFichierXML(xmlPath.toString(), xsdPath.toString());

        assertEquals("Fichier XML valide : " + xmlPath.toAbsolutePath(), message);
    }

    @Test
    void validerFichierXML_retourneListeErreurs_quandXmlInvalide() throws IOException {
        Path xmlPath = RESSOURCES.resolve("order-invalid.xml");
        Path xsdPath = RESSOURCES.resolve("simple-order.xsd");

        String message = XmlValidator.validerFichierXML(xmlPath.toString(), xsdPath.toString());

        assertTrue(message.startsWith("Fichier XML invalide : " + xmlPath.toAbsolutePath()));
        assertTrue(message.contains("Erreurs détectées :"));
        assertTrue(message.contains("Erreur"));
    }

    @Test
    void validerFichierXML_declencheIOException_quandXmlAbsent() {
        Path xsdPath = RESSOURCES.resolve("simple-order.xsd");

        assertThrows(IOException.class, () ->
                XmlValidator.validerFichierXML("src/test/resources/schema/inexistant.xml", xsdPath.toString()));
    }

    @Test
    void validerFichierXML_declencheIllegalArgumentException_quandCheminVide() {
        Path xsdPath = RESSOURCES.resolve("simple-order.xsd");

        assertThrows(IllegalArgumentException.class, () -> XmlValidator.validerFichierXML("  ", xsdPath.toString()));
        assertThrows(IllegalArgumentException.class, () -> XmlValidator.validerFichierXML(RESSOURCES.resolve("order-valid.xml").toString(), ""));
    }
}
