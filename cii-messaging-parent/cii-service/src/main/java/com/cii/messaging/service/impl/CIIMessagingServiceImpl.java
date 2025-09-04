package com.cii.messaging.service.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.reader.*;
import com.cii.messaging.writer.*;
import com.cii.messaging.validator.*;
import com.cii.messaging.validator.impl.CompositeValidator;
import com.cii.messaging.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CIIMessagingServiceImpl implements CIIMessagingService {
    private static final Logger logger = LoggerFactory.getLogger(CIIMessagingServiceImpl.class);
    
    private final ObjectMapper jsonMapper;
    private final CIIValidator validator;
    
    public CIIMessagingServiceImpl() {
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.findAndRegisterModules(); // For Java 8 time support
        this.validator = new CompositeValidator();
    }
    
    @Override
    public CIIMessage readMessage(File xmlFile) throws ServiceException {
        try {
            // Auto-detect message type
            CIIReader reader = CIIReaderFactory.createReader(xmlFile.toPath());
            return reader.read(xmlFile);
        } catch (Exception e) {
            throw new ServiceException("Échec de la lecture du message à partir du fichier : " + xmlFile.getName(), e);
        }
    }
    
    @Override
    public CIIMessage readMessage(InputStream inputStream, MessageType expectedType) throws ServiceException {
        try {
            CIIReader reader = CIIReaderFactory.createReader(expectedType);
            return reader.read(inputStream);
        } catch (Exception e) {
            throw new ServiceException("Échec de la lecture du message depuis le flux", e);
        }
    }
    
    @Override
    public CIIMessage readMessage(String xmlContent) throws ServiceException {
        try {
            CIIReader reader = CIIReaderFactory.createReader(xmlContent);
            return reader.read(xmlContent);
        } catch (Exception e) {
            throw new ServiceException("Échec de la lecture du message à partir du contenu XML", e);
        }
    }
    
    @Override
    public void writeMessage(CIIMessage message, File outputFile) throws ServiceException {
        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            writer.write(message, outputFile);
        } catch (Exception e) {
            throw new ServiceException("Échec de l'écriture du message dans le fichier : " + outputFile.getName(), e);
        }
    }
    
    @Override
    public void writeMessage(CIIMessage message, OutputStream outputStream) throws ServiceException {
        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            writer.write(message, outputStream);
        } catch (Exception e) {
            throw new ServiceException("Échec de l'écriture du message dans le flux", e);
        }
    }
    
    @Override
    public String writeMessageToString(CIIMessage message) throws ServiceException {
        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            return writer.writeToString(message);
        } catch (Exception e) {
            throw new ServiceException("Échec de l'écriture du message en chaîne", e);
        }
    }
    
    @Override
    public ValidationResult validateMessage(File xmlFile) throws ServiceException {
        try {
            return validator.validate(xmlFile);
        } catch (Exception e) {
            throw new ServiceException("Échec de la validation du fichier : " + xmlFile.getName(), e);
        }
    }
    
    @Override
    public ValidationResult validateMessage(String xmlContent) throws ServiceException {
        try {
            return validator.validate(xmlContent);
        } catch (Exception e) {
            throw new ServiceException("Échec de la validation du contenu XML", e);
        }
    }
    
    @Override
    public ValidationResult validateMessage(CIIMessage message) throws ServiceException {
        try {
            // First convert to XML, then validate
            String xml = writeMessageToString(message);
            return validator.validate(xml);
        } catch (Exception e) {
            throw new ServiceException("Échec de la validation du message", e);
        }
    }

    @Override
    public void setSchemaVersion(SchemaVersion version) {
        validator.setSchemaVersion(version);
    }
    
    @Override
    public String convertToJson(CIIMessage message) throws ServiceException {
        try {
            return jsonMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new ServiceException("Échec de la conversion du message en JSON", e);
        }
    }
    
    @Override
    public CIIMessage convertFromJson(String json, MessageType messageType) throws ServiceException {
        try {
            CIIMessage message = jsonMapper.readValue(json, CIIMessage.class);
            // Ensure message type is set correctly
            if (message.getMessageType() == null) {
                message.setMessageType(messageType);
            } else if (message.getMessageType() != messageType) {
                throw new ServiceException("Type de message incohérent : attendu " + messageType + " mais trouvé " + message.getMessageType());
            }
            return message;
        } catch (Exception e) {
            throw new ServiceException("Échec de la conversion du JSON en message", e);
        }
    }
    
    @Override
    public CIIMessage createInvoiceResponse(CIIMessage order) throws ServiceException {
        if (order.getMessageType() != MessageType.ORDER) {
            throw new ServiceException("L'entrée doit être un message ORDER");
        }
        
        try {
            CIIMessage invoice = CIIMessage.builder()
                    .messageId(generateMessageId())
                    .messageType(MessageType.INVOICE)
                    .creationDateTime(LocalDateTime.now())
                    .senderPartyId(order.getReceiverPartyId()) // Swap sender/receiver
                    .receiverPartyId(order.getSenderPartyId())
                    .header(createInvoiceHeader(order.getHeader()))
                    .lineItems(copyLineItems(order.getLineItems()))
                    .totals(calculateTotals(order.getLineItems()))
                    .build();
            
            return invoice;
        } catch (Exception e) {
            throw new ServiceException("Échec de la création de la facture à partir de l'ordre", e);
        }
    }
    
    @Override
    public CIIMessage createDespatchAdvice(CIIMessage order) throws ServiceException {
        if (order.getMessageType() != MessageType.ORDER) {
            throw new ServiceException("L'entrée doit être un message ORDER");
        }
        
        try {
            CIIMessage desadv = CIIMessage.builder()
                    .messageId(generateMessageId())
                    .messageType(MessageType.DESADV)
                    .creationDateTime(LocalDateTime.now())
                    .senderPartyId(order.getReceiverPartyId())
                    .receiverPartyId(order.getSenderPartyId())
                    .header(createDesadvHeader(order.getHeader()))
                    .lineItems(copyLineItems(order.getLineItems()))
                    .build();
            
            return desadv;
        } catch (Exception e) {
            throw new ServiceException("Échec de la création de l'avis d'expédition à partir de l'ordre", e);
        }
    }
    
    @Override
    public CIIMessage createOrderResponse(CIIMessage order, OrderResponseType responseType) throws ServiceException {
        if (order.getMessageType() != MessageType.ORDER) {
            throw new ServiceException("L'entrée doit être un message ORDER");
        }
        
        try {
            CIIMessage ordersp = CIIMessage.builder()
                    .messageId(generateMessageId())
                    .messageType(MessageType.ORDERSP)
                    .creationDateTime(LocalDateTime.now())
                    .senderPartyId(order.getReceiverPartyId())
                    .receiverPartyId(order.getSenderPartyId())
                    .header(createOrderResponseHeader(order.getHeader(), responseType))
                    .lineItems(copyLineItems(order.getLineItems()))
                    .build();
            
            return ordersp;
        } catch (Exception e) {
            throw new ServiceException("Échec de la création de la réponse à l'ordre", e);
        }
    }
    
    // Helper methods

    private String generateMessageId() {
        return "MSG-" + UUID.randomUUID().toString();
    }
    
    private DocumentHeader createInvoiceHeader(DocumentHeader orderHeader) {
        if (orderHeader == null) {
            return DocumentHeader.builder().build();
        }
        
        return DocumentHeader.builder()
                .documentNumber("INV-" + UUID.randomUUID().toString().substring(0, 8))
                .documentDate(LocalDate.now())
                .buyerReference(orderHeader.getBuyerReference())
                .sellerReference(orderHeader.getSellerReference())
                .contractReference(orderHeader.getContractReference())
                .currency(orderHeader.getCurrency())
                .paymentTerms(orderHeader.getPaymentTerms())
                .delivery(orderHeader.getDelivery())
                .build();
    }
    
    private DocumentHeader createDesadvHeader(DocumentHeader orderHeader) {
        if (orderHeader == null) {
            return DocumentHeader.builder().build();
        }
        
        return DocumentHeader.builder()
                .documentNumber("DES-" + UUID.randomUUID().toString().substring(0, 8))
                .documentDate(LocalDate.now())
                .buyerReference(orderHeader.getBuyerReference())
                .sellerReference(orderHeader.getSellerReference())
                .delivery(orderHeader.getDelivery())
                .build();
    }
    
    private DocumentHeader createOrderResponseHeader(DocumentHeader orderHeader, OrderResponseType responseType) {
        if (orderHeader == null) {
            return DocumentHeader.builder().build();
        }
        
        return DocumentHeader.builder()
                .documentNumber("ORD-RSP-" + UUID.randomUUID().toString().substring(0, 8))
                .documentDate(LocalDate.now())
                .buyerReference(orderHeader.getBuyerReference())
                .sellerReference(orderHeader.getSellerReference())
                .build();
    }
    
    private List<LineItem> copyLineItems(List<LineItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }
        
        List<LineItem> copied = new ArrayList<>();
        for (LineItem item : items) {
            copied.add(LineItem.builder()
                    .lineNumber(item.getLineNumber())
                    .productId(item.getProductId())
                    .description(item.getDescription())
                    .quantity(item.getQuantity())
                    .unitCode(item.getUnitCode())
                    .unitPrice(item.getUnitPrice())
                    .lineAmount(item.getLineAmount())
                    .taxRate(item.getTaxRate())
                    .taxCategory(item.getTaxCategory())
                    .build());
        }
        return copied;
    }
    
    private TotalsInformation calculateTotals(List<LineItem> items) {
        if (items == null || items.isEmpty()) {
            return TotalsInformation.builder()
                    .lineTotalAmount(BigDecimal.ZERO)
                    .taxTotalAmount(BigDecimal.ZERO)
                    .grandTotalAmount(BigDecimal.ZERO)
                    .build();
        }
        
        BigDecimal lineTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        
        for (LineItem item : items) {
            if (item.getLineAmount() != null) {
                lineTotal = lineTotal.add(item.getLineAmount());
                
                if (item.getTaxRate() != null) {
                    BigDecimal tax = item.getLineAmount()
                            .multiply(item.getTaxRate())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    taxTotal = taxTotal.add(tax).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        
        BigDecimal grandTotal = lineTotal.add(taxTotal);
        
        return TotalsInformation.builder()
                .lineTotalAmount(lineTotal)
                .taxBasisAmount(lineTotal)
                .taxTotalAmount(taxTotal)
                .grandTotalAmount(grandTotal)
                .duePayableAmount(grandTotal)
                .build();
    }
}
