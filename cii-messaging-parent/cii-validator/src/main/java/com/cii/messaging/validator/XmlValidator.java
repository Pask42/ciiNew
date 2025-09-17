package com.cii.messaging.validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Utilitaire responsable de la validation de fichiers XML à partir d'un schéma XSD.
 */
public final class XmlValidator {

    private XmlValidator() {
        // utilitaire
    }

    /**
     * Valide un fichier XML en s'appuyant sur un schéma XSD et retourne un message de synthèse.
     *
     * @param cheminFichier chemin vers le fichier XML à contrôler
     * @param cheminXSD     chemin vers le schéma XSD à utiliser
     * @return un message décrivant le résultat de la validation
     * @throws IOException si l'accès aux fichiers échoue ou si le schéma est invalide
     */
    public static String validerFichierXML(String cheminFichier, String cheminXSD) throws IOException {
        Objects.requireNonNull(cheminFichier, "cheminFichier");
        Objects.requireNonNull(cheminXSD, "cheminXSD");

        if (cheminFichier.isBlank()) {
            throw new IllegalArgumentException("Le chemin du fichier XML ne peut pas être vide");
        }
        if (cheminXSD.isBlank()) {
            throw new IllegalArgumentException("Le chemin du fichier XSD ne peut pas être vide");
        }

        Path xmlPath = Path.of(cheminFichier);
        Path xsdPath = Path.of(cheminXSD);

        verifierFichier(xmlPath, "XML");
        verifierFichier(xsdPath, "XSD");

        Schema schema = chargerSchema(xsdPath);
        Validator validator = schema.newValidator();
        List<String> erreurs = new ArrayList<>();
        validator.setErrorHandler(new CollectingErrorHandler(erreurs));

        try {
            validator.validate(new StreamSource(xmlPath.toFile()));
        } catch (SAXException e) {
            if (erreurs.isEmpty()) {
                erreurs.add("Erreur de validation : " + e.getMessage());
            }
        }

        if (erreurs.isEmpty()) {
            return "Fichier XML valide : " + xmlPath.toAbsolutePath();
        }

        String lineSeparator = System.lineSeparator();
        StringBuilder message = new StringBuilder();
        message.append("Fichier XML invalide : ")
                .append(xmlPath.toAbsolutePath())
                .append(lineSeparator)
                .append("Erreurs détectées :");
        for (String erreur : erreurs) {
            message.append(lineSeparator)
                    .append(" - ")
                    .append(erreur);
        }
        return message.toString();
    }

    private static void verifierFichier(Path path, String type) throws IOException {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IOException("Fichier " + type + " introuvable : " + path);
        }
    }

    private static Schema chargerSchema(Path xsdPath) throws IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            return schemaFactory.newSchema(xsdPath.toFile());
        } catch (SAXException e) {
            throw new IOException("Schéma XSD invalide : " + xsdPath.toAbsolutePath(), e);
        }
    }

    private static final class CollectingErrorHandler implements ErrorHandler {

        private final List<String> erreurs;

        private CollectingErrorHandler(List<String> erreurs) {
            this.erreurs = erreurs;
        }

        @Override
        public void warning(SAXParseException exception) {
            erreurs.add(formatMessage("Avertissement", exception));
        }

        @Override
        public void error(SAXParseException exception) {
            erreurs.add(formatMessage("Erreur", exception));
        }

        @Override
        public void fatalError(SAXParseException exception) {
            erreurs.add(formatMessage("Erreur fatale", exception));
        }

        private String formatMessage(String niveau, SAXParseException exception) {
            StringBuilder builder = new StringBuilder(niveau);
            int ligne = exception.getLineNumber();
            int colonne = exception.getColumnNumber();
            if (ligne > 0 || colonne > 0) {
                builder.append(" (ligne ");
                builder.append(Math.max(ligne, 0));
                if (colonne > 0) {
                    builder.append(", colonne ").append(colonne);
                }
                builder.append(')');
            }
            builder.append(" : ").append(exception.getMessage());
            return builder.toString();
        }
    }
}
