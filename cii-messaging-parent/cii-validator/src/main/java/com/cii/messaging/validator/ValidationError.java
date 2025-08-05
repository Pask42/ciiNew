package com.cii.messaging.validator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationError {
    private String message;
    private String location;
    private int lineNumber;
    private int columnNumber;
    private ErrorSeverity severity;
    private String rule;
    
    public enum ErrorSeverity {
        ERROR,
        FATAL
    }
}
