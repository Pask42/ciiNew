package com.cii.messaging.service;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.validator.ValidationResult;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public interface CIIMessagingService {
    // Reading operations
    CIIMessage readMessage(File xmlFile) throws ServiceException;
    CIIMessage readMessage(InputStream inputStream, MessageType expectedType) throws ServiceException;
    CIIMessage readMessage(String xmlContent) throws ServiceException;
    
    // Writing operations
    void writeMessage(CIIMessage message, File outputFile) throws ServiceException;
    void writeMessage(CIIMessage message, OutputStream outputStream) throws ServiceException;
    String writeMessageToString(CIIMessage message) throws ServiceException;
    
    // Validation operations
    ValidationResult validateMessage(File xmlFile) throws ServiceException;
    ValidationResult validateMessage(String xmlContent) throws ServiceException;
    ValidationResult validateMessage(CIIMessage message) throws ServiceException;
    
    // Conversion operations
    String convertToJson(CIIMessage message) throws ServiceException;
    CIIMessage convertFromJson(String json, MessageType messageType) throws ServiceException;
    
    // Business operations
    CIIMessage createInvoiceResponse(CIIMessage order) throws ServiceException;
    CIIMessage createDespatchAdvice(CIIMessage order) throws ServiceException;
    CIIMessage createOrderResponse(CIIMessage order, OrderResponseType responseType) throws ServiceException;
}
