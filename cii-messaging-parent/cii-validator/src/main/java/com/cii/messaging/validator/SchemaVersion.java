package com.cii.messaging.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public enum SchemaVersion {
    D23B("D23B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"),
    D24A("D24A", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100");

    private static final String PROPERTY_KEY = "unece.version";
    private static final String ENVIRONMENT_KEY = "UNECE_VERSION";
    private static final String VERSION_RESOURCE = "unece-version.properties";

    private final String version;
    private final String namespace;

    SchemaVersion(String version, String namespace) {
        this.version = version;
        this.namespace = namespace;
    }

    public String getVersion() {
        return version;
    }

    public String getNamespace() {
        return namespace;
    }

    public static SchemaVersion fromString(String version) {
        for (SchemaVersion v : values()) {
            if (v.version.equalsIgnoreCase(version)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Version de schéma non prise en charge : " + version);
    }

    public static SchemaVersion getDefault() {
        String configuredVersion = resolveConfiguredVersion();
        return fromString(configuredVersion);
    }

    private static String resolveConfiguredVersion() {
        String configuredVersion = System.getProperty(PROPERTY_KEY);
        if (configuredVersion == null || configuredVersion.isBlank()) {
            configuredVersion = System.getenv(ENVIRONMENT_KEY);
        }
        if (configuredVersion == null || configuredVersion.isBlank()) {
            configuredVersion = readVersionFromResource();
        }
        if (configuredVersion == null || configuredVersion.isBlank()) {
            configuredVersion = D23B.version;
        }
        return configuredVersion.trim();
    }

    private static String readVersionFromResource() {
        try (InputStream inputStream = SchemaVersion.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE)) {
            if (inputStream == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(PROPERTY_KEY);
        } catch (IOException e) {
            return null;
        }
    }
}
