package com.cii.messaging.validator;

public enum SchemaVersion {
    D23B("D23B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"),
    D24A("D24A", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100");

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
        String configuredVersion = System.getProperty("unece.version");
        if (configuredVersion == null || configuredVersion.isBlank()) {
            configuredVersion = System.getenv("UNECE_VERSION");
        }
        if (configuredVersion == null || configuredVersion.isBlank()) {
            configuredVersion = "D23B";
        }
        return fromString(configuredVersion.trim());
    }
}
