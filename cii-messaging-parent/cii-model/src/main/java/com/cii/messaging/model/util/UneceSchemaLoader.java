package com.cii.messaging.model.util;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Utility to load UN/CEFACT XSD schemas based on the {@code unece.version}
 * parameter.
 * <p>
 * Version resolution order:
 * </p>
 * <ol>
 * <li>System property {@code unece.version}</li>
 * <li>Environment variable {@code UNECE_VERSION}</li>
 * <li>Default {@code D23B}</li>
 * </ol>
 */
public final class UneceSchemaLoader {
	public static final String PROPERTY = "unece.version";
	public static final String ENV = "UNECE_VERSION";
	private static final String DEFAULT_VERSION;

	static {
		String version = null;
		try (InputStream in = UneceSchemaLoader.class.getResourceAsStream("/unece-version.properties")) {
			if (in != null) {
				Properties props = new Properties();
				props.load(in);
				version = props.getProperty(PROPERTY);
			}
		} catch (IOException ignored) {
		}
		DEFAULT_VERSION = (version == null || version.isEmpty()) ? "D23B" : version;
	}

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
		String baseDir = switch (version) {
		case "D16B" -> String.format("/xsd/%s/uncefact/data/standard/", version);
		case "D23B" -> String.format("/xsd/%s/", version);
		default -> String.format("/xsd/%s/", version);
		};
		String resolvedName = schemaFile;
		URL xsd = UneceSchemaLoader.class.getResource(baseDir + resolvedName);
		if (xsd == null) {
			// si aucun fichier exact, tenter avec suffixe de version connu
			String prefix = schemaFile.endsWith(".xsd") ? schemaFile.substring(0, schemaFile.length() - 4) : schemaFile;
			String suffix = switch (version) {
			case "D16B" -> switch (prefix) {
			case "CrossIndustryInvoice" -> "13p1";
			case "CrossIndustryOrder" -> "12p1";
			case "CrossIndustryOrderChange" -> "12p1";
			case "CrossIndustryOrderResponse" -> "12p1";
			case "CrossIndustryDespatchAdvice" -> "12p1";
			default -> null;
			};
			case "D23B" -> switch (prefix) {
			case "CrossIndustryInvoice", "CrossIndustryOrder", "CrossIndustryOrderChange", "CrossIndustryOrderResponse",
					"CrossIndustryDespatchAdvice" ->
				"100p" + version;
			default -> null;
			};
			default -> null;
			};

			if (suffix != null) {
				resolvedName = prefix + "_" + suffix + ".xsd";
				xsd = UneceSchemaLoader.class.getResource(baseDir + resolvedName);
			}
		}

		if (xsd == null) {
			throw new IllegalArgumentException(
					"Schéma XSD introuvable pour la version " + version + " à " + baseDir + resolvedName);
		}

		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			try {
				factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "file,jar,jar:file");
				factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar,jar:file");
			} catch (Exception ignored) {
				// Certains parseurs ne supportent pas ces propriétés
			}
			return factory.newSchema(xsd);
		} catch (Exception e) {
			throw new IllegalArgumentException("Impossible de charger le schéma depuis " + baseDir + resolvedName, e);
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
