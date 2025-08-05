package com.cii.messaging.service;

public enum OrderResponseType {
    ACCEPTED("29", "Order accepted without amendment"),
    ACCEPTED_WITH_AMENDMENT("30", "Order accepted with amendment"),
    NOT_ACCEPTED("27", "Order not accepted"),
    PENDING("AB", "Order response pending");
    
    private final String code;
    private final String description;
    
    OrderResponseType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}
