package com.cii.messaging.service.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.reader.*;
import com.cii.messaging.writer.*;
import com.cii.messaging.validator.*;
import com.cii.messaging.validator.impl.CompositeValidator;
import com.cii.messaging.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CIIMessagingServiceImpl implements CIIMessagingService {
    private static final Logger logger = LoggerFactory.getLogger(CIIMessagingServiceImpl.class);
    
    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;
    private final CIIValidator validator;
    
    public CIIMessagingServiceImpl() {
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.findAndRegisterModules(); // For Java 8 time support
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.findAndRegisterModules();
        this.validator = new CompositeValidator();
    }
    
    @Override
    public CIIMessage readMessage(File xmlFile) throws ServiceException {
        try {
            // Auto-detect message type
            CIIReader reader = CIIReaderFactory.createReader(readFileContent(xmlFile));
            return reader.read(xmlFile);
        } catch (Exception e) {
            throw new ServiceException("Failed to read message from file: " + xmlFile.getName(), e);
        }
    }
    
    @Override
    public CIIMessage readMessage(InputStream inputStream, MessageType expectedType) throws ServiceException {
        try {
            CIIReader reader = CIIReaderFactory.createReader(expectedType);
            return reader.read(inputStream);
        } catch (Exception e) {
            throw new ServiceException("Failed to read message from stream", e);
        }
    }
    
    @Override
    public CIIMessage readMessage(String xmlContent) throws ServiceException {
        try {
            CIIReader reader = CIIReaderFactory.createReader(xmlContent);
            return reader.read(xmlContent);
        } catch (Exception e) {
            throw new ServiceException("Failed to read message from XML content", e);
        }
    }
    
    @Override
    public void writeMessage(CIIMessage message, File outputFile) throws ServiceException {
        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            writer.write(message, outputFile);
        } catch (Exception e) {
            throw new ServiceException("Failed to write message to file: " + outputFile.getName(), e);
        }
    }
    
    @Override
    public void writeMessage(CIIMessage message, OutputStream outputStream) throws ServiceException {
        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            writer.write(message, outputStream);
        } catch (Exception e) {
            throw new ServiceException("Failed to write message to stream", e);
        }
    }
    
    @Override
    public String writeMessageToString(CIIMessage message) throws ServiceException {
        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            return writer.writeToString(message);
        } catch (Exception e) {
            throw new ServiceException("Failed to write message to string", e);
        }
    }
    
    @Override
    public ValidationResult validateMessage(File xmlFile) throws ServiceException {
        try {
            return validator.validate(xmlFile);
        } catch (Exception e) {
            throw new ServiceException("Failed to validate file: " + xmlFile.getName(), e);
        }
    }
    
    @Override
    public ValidationResult validateMessage(String xmlContent) throws ServiceException {
        try {
            return validator.validate(xmlContent);
        } catch (Exception e) {
            throw new ServiceException("Failed to validate XML content", e);
        }
    }
    
    @Override
    public ValidationResult validateMessage(CIIMessage message) throws ServiceException {
        try {
            // First convert to XML, then validate
            String xml = writeMessageToString(message);
            return validator.validate(xml);
        } catch (Exception e) {
            throw new ServiceException("Failed to validate message", e);
        }
    }
    
    @Override
    public String convertToJson(CIIMessage message) throws ServiceException {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
        } catch (Exception e) {
            throw new ServiceException("Failed to convert message to JSON", e);
        }
    }
    
    @Override
    public CIIMessage convertFromJson(String json, MessageType messageType) throws ServiceException {
        try {
            CIIMessage message = jsonMapper.readValue(json, CIIMessage.class);
            // Ensure message type is set correctly
            if (message.getMessageType() == null) {
                message.setMessageType(messageType);
            }
            return message;
        } catch (Exception e) {
            throw new ServiceException("Failed to convert JSON to message", e);
        }
    }
    
    @Override
    public CIIMessage createInvoiceResponse(CIIMessage order) throws ServiceException {
        if (order.getMessageType() != MessageType.ORDER) {
            throw new ServiceException("Input must be an ORDER message");
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
            throw new ServiceException("Failed to create invoice from order", e);
        }
    }
    
    @Override
    public CIIMessage createDespatchAdvice(CIIMessage order) throws ServiceException {
        if (order.getMessageType() != MessageType.ORDER) {
            throw new ServiceException("Input must be an ORDER message");
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
            throw new ServiceException("Failed to create despatch advice from order", e);
        }
    }
    
    @Override
    public CIIMessage createOrderResponse(CIIMessage order, OrderResponseType responseType) throws ServiceException {
        if (order.getMessageType() != MessageType.ORDER) {
            throw new ServiceException("Input must be an ORDER message");
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
            throw new ServiceException("Failed to create order response", e);
        }
    }
    
    // Helper methods
    
    private String readFileContent(File file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
    
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
                            .divide(BigDecimal.valueOf(100));
                    taxTotal = taxTotal.add(tax);
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
