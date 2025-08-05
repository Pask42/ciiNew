package com.cii.messaging.reader;

public class CIIReaderException extends Exception {
    public CIIReaderException(String message) {
        super(message);
    }
    
    public CIIReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
