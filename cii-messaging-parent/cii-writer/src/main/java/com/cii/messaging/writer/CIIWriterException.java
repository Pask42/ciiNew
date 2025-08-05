package com.cii.messaging.writer;

public class CIIWriterException extends Exception {
    public CIIWriterException(String message) {
        super(message);
    }
    
    public CIIWriterException(String message, Throwable cause) {
        super(message, cause);
    }
}
