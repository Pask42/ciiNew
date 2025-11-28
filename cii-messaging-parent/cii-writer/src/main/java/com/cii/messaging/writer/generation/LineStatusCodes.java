package com.cii.messaging.writer.generation;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Référentiel des codes UNECE (ActionCode/1229) utilisables pour {@code LineStatusCode}.
 *
 * <p>La liste est chargée depuis le jeu de codes UNECE embarqué afin de rester synchronisée avec
 * le standard. En cas d'indisponibilité du fichier XSD, un repli complet (1..119) est proposé.</p>
 */
public final class LineStatusCodes {

    private static final String ACTION_CODE_RESOURCE =
            "/xsd/D23B/CrossIndustryOrderResponse_100pD23B_urn_un_unece_uncefact_codelist_standard_"
                    + "UNECE_ActionCode_D23A.xsd";
    private static final Set<String> VALID_CODES;
    private static final Set<String> FALLBACK_CODES = buildFallbackCodes();

    static {
        VALID_CODES = loadCodes();
    }

    private LineStatusCodes() {
        // utilitaire
    }

    /**
     * Vérifie si le code fourni appartient au référentiel ActionCode (1229) UNECE.
     *
     * @param code valeur candidate (peut être {@code null})
     * @return {@code true} si le code est reconnu
     */
    public static boolean isValid(String code) {
        return code != null && VALID_CODES.contains(code);
    }

    /**
     * Liste immuable des codes valides afin de documenter les messages d'erreur.
     *
     * @return ensemble des codes disponibles
     */
    public static Set<String> validCodes() {
        return VALID_CODES;
    }

    private static Set<String> loadCodes() {
        try (InputStream stream = LineStatusCodes.class.getResourceAsStream(ACTION_CODE_RESOURCE)) {
            if (stream == null) {
                return FALLBACK_CODES;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            disableExternalEntities(factory);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(stream);
            NodeList nodes = document.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "enumeration");
            if (nodes == null || nodes.getLength() == 0) {
                return FALLBACK_CODES;
            }

            Set<String> codes = new LinkedHashSet<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                String value = nodes.item(i).getAttributes().getNamedItem("value").getNodeValue();
                if (value != null && !value.isBlank()) {
                    codes.add(value.trim());
                }
            }
            return Collections.unmodifiableSet(codes);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Impossible d'initialiser le parseur XML pour les codes d'état de ligne", e);
        } catch (SAXException e) {
            throw new IllegalStateException("Fichier XSD UNECE invalide pour les codes d'état de ligne", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire la liste des codes d'état de ligne UNECE", e);
        }
    }

    private static void disableExternalEntities(DocumentBuilderFactory factory) {
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (IllegalArgumentException ignored) {
            // propriété non supportée selon l'implémentation
        }
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // idem
        }
    }

    private static Set<String> buildFallbackCodes() {
        Set<String> codes = IntStream.rangeClosed(1, 119)
                .mapToObj(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(codes);
    }
}
