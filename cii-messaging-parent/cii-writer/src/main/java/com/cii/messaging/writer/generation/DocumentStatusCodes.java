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
 * Liste utilitaire des codes UNECE valides pour {@code DocumentStatusCode} dans un ORDER_RESPONSE.
 *
 * <p>Les codes correspondent à la liste officielle « Document Status Code » disponible dans les schémas
 * CrossIndustryOrderResponse D23B. La liste est chargée dynamiquement depuis le fichier XSD embarqué afin de garantir
 * une conformité stricte au standard UNECE, y compris lorsque la taxonomie évolue.</p>
 */
public final class DocumentStatusCodes {

    /** Code indiquant une acceptation de la commande sans modification (valeur par défaut). */
    public static final String DEFAULT_ACKNOWLEDGEMENT_CODE = "1";

    private static final String DOCUMENT_STATUS_RESOURCE =
            "/xsd/D23B/CrossIndustryOrderResponse_100pD23B_urn_un_unece_uncefact_codelist_standard_"
                    + "UNECE_DocumentStatusCode_D23A.xsd";
    private static final Set<String> VALID_CODES;
    private static final Set<String> FALLBACK_CODES = buildFallbackCodes();

    static {
        VALID_CODES = loadCodes();
    }

    private DocumentStatusCodes() {
        // utilitaire
    }

    /**
     * Indique si le code fourni figure dans la liste officielle UNECE.
     *
     * @param code valeur à tester (peut être {@code null})
     * @return {@code true} si le code est reconnu, sinon {@code false}
     */
    public static boolean isValid(String code) {
        return code != null && VALID_CODES.contains(code);
    }

    /**
     * Retourne la liste immuable des codes acceptés afin d'afficher un message d'erreur explicite.
     *
     * @return ensemble trié des codes valides
     */
    public static Set<String> validCodes() {
        return VALID_CODES;
    }

    private static Set<String> loadCodes() {
        try (InputStream stream = DocumentStatusCodes.class.getResourceAsStream(DOCUMENT_STATUS_RESOURCE)) {
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
            throw new IllegalStateException("Impossible d'initialiser le parseur XML pour les codes d'accusé UNECE", e);
        } catch (SAXException e) {
            throw new IllegalStateException("Fichier XSD UNECE invalide pour les codes d'accusé", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire la liste des codes UNECE", e);
        }
    }

    private static void disableExternalEntities(DocumentBuilderFactory factory) {
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (IllegalArgumentException ignored) {
            // propriété non supportée selon l'implémentation, on ignore.
        }
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // idem.
        }
    }

    private static Set<String> buildFallbackCodes() {
        Set<String> codes = IntStream.rangeClosed(1, 51)
                .mapToObj(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(codes);
    }
}
