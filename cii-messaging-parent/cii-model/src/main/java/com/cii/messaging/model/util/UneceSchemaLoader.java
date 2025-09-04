package com.cii.messaging.model.util;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;

/**
 * Utility to load UN/CEFACT XSD schemas based on the {@code unece.version} parameter.
 * <p>Version resolution order:</p>
 * <ol>
 *   <li>System property {@code unece.version}</li>
 *   <li>Environment variable {@code UNECE_VERSION}</li>
 *   <li>Default {@code D23B}</li>
 * </ol>
 */
public final class UneceSchemaLoader {
    public static final String PROPERTY = "unece.version";
    public static final String ENV = "UNECE_VERSION";
    public static final String DEFAULT_VERSION = "D23B";

    private UneceSchemaLoader() {
    }

    /**
     * Loads a schema file for the resolved UN/CEFACT version.
     *
     * @param schemaFile the schema file name (e.g. "CrossIndustryInvoice.xsd")
     * @return the loaded {@link Schema}
     * @throws IllegalArgumentException if the schema cannot be found
     */
    public static Schema loadSchema(String schemaFile) {
        String version = resolveVersion();
        String resourcePath = String.format("/xsd/%s/uncefact/data/standard/%s", version, schemaFile);
        InputStream xsd = UneceSchemaLoader.class.getResourceAsStream(resourcePath);
        if (xsd == null) {
            throw new IllegalArgumentException("XSD schema not found for version " + version + " at " + resourcePath);
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return factory.newSchema(new StreamSource(xsd));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load schema from " + resourcePath, e);
        }
    }

    /**
     * Resolves the UNECE version from system properties/environment variables.
     */
    public static String resolveVersion() {
        String version = System.getProperty(PROPERTY);
        if (version == null || version.isEmpty()) {
            version = System.getenv(ENV);
        }
        if (version == null || version.isEmpty()) {
            version = DEFAULT_VERSION;
        }
        return version;
    }
}
