package com.cii.messaging.validator;

import com.cii.messaging.model.util.UneceSchemaLoader;

public enum SchemaVersion {
    D16B("D16B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:16B"),
    D20B("D20B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:20B"),
    D21B("D21B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:21B"),
    D23B("D23B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:23B");

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
        throw new IllegalArgumentException("Unsupported schema version: " + version);
    }

    public static SchemaVersion getDefault() {
        return fromString(UneceSchemaLoader.resolveVersion());
    }
}
