package com.cii.messaging.validator;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.xml.sax.SAXException;

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
        String version = SchemaVersion.getDefault().getVersion();
        String baseName = xsdName.endsWith(".xsd") ? xsdName.substring(0, xsdName.length() - 4) : xsdName;
        String resourcePath = String.format("/xsd/%s/%s_100p%s.xsd", version, baseName, version);

        URL url = UneceSchemaLoader.class.getResource(resourcePath);
        if (url == null) {
            throw new IOException("Schéma introuvable : " + resourcePath);
        }

        try (InputStream is = url.openStream()) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            StreamSource source = new StreamSource(is, url.toExternalForm());
            return factory.newSchema(source);
        }
    }
}

