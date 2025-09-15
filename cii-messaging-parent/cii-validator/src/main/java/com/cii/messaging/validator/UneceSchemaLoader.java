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
 * Utility class to load UNECE XSD schemas according to the configured version.
 */
public final class UneceSchemaLoader {

    private UneceSchemaLoader() {
        // Utility class
    }

    /**
     * Loads an XSD schema from the classpath for the current UNECE version.
     * <p>
     * The loader automatically appends the "_100pVERSION" suffix to the provided
     * XSD name and searches under <code>/xsd/VERSION/</code>.
     * </p>
     *
     * @param xsdName base XSD file name (e.g. "CrossIndustryInvoice.xsd")
     * @return loaded {@link Schema}
     * @throws IOException   if the resource cannot be found
     * @throws SAXException  if the XSD cannot be parsed
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

