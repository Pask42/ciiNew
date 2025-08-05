package com.cii.messaging.validator;

public enum SchemaVersion {
    D16B("D16B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:16B"),
    D20B("D20B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:20B"),
    D21B("D21B", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:21B");
    
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
}
