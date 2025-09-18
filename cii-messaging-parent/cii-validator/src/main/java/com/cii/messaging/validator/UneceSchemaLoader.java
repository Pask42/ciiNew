package com.cii.messaging.validator;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Classe utilitaire chargée de charger les schémas XSD UNECE selon la version configurée.
 */
public final class UneceSchemaLoader {

    private UneceSchemaLoader() {
        // classe utilitaire
    }

    /**
     * Charge un schéma XSD depuis le classpath pour la version UNECE courante.
     * <p>
     * Le chargeur ajoute automatiquement le suffixe « _100pVERSION » au nom
     * fourni et recherche sous <code>/xsd/VERSION/</code>.
     * </p>
     *
     * @param xsdName nom de base du fichier XSD (ex. « CrossIndustryInvoice.xsd »)
     * @return le {@link Schema} chargé
     * @throws IOException  si la ressource est introuvable
     * @throws SAXException si le XSD ne peut pas être analysé
     */
    public static Schema loadSchema(String xsdName) throws IOException, SAXException {
        return loadSchema(xsdName, SchemaVersion.getDefault());
    }

    public static Schema loadSchema(String xsdName, SchemaVersion version) throws IOException, SAXException {
        Objects.requireNonNull(xsdName, "xsdName");
        Objects.requireNonNull(version, "version");

        String uneceVersion = version.getVersion();
        String baseName = xsdName.endsWith(".xsd") ? xsdName.substring(0, xsdName.length() - 4) : xsdName;
        String resourcePath = String.format("/xsd/%s/%s_100p%s.xsd", uneceVersion, baseName, uneceVersion);

        URL url = UneceSchemaLoader.class.getResource(resourcePath);
        if (url == null) {
            throw new IOException("Schéma introuvable : " + resourcePath);
        }

        try (InputStream is = url.openStream()) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            disableExternalAccess(factory);
            StreamSource source = new StreamSource(is, url.toExternalForm());
            return factory.newSchema(source);
        }
    }

    private static void disableExternalAccess(SchemaFactory factory) {
        try {
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
            // Certains parseurs ne supportent pas ces propriétés : on ignore silencieusement.
        }
    }
}

