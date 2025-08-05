package com.cii.messaging.model;

public enum MessageType {
    ORDER("Order"),
    ORDERSP("Order Response"),
    DESADV("Despatch Advice"),
    INVOICE("Invoice");
    
    private final String description;
    
    MessageType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
