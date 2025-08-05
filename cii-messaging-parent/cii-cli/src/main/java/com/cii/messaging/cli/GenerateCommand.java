package com.cii.messaging.cli;

import com.cii.messaging.model.*;
import com.cii.messaging.service.CIIMessagingService;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;
import picocli.CommandLine.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.Callable;

@Command(
    name = "generate",
    description = "Generate CII messages (INVOICE, DESADV, ORDERSP)"
)
public class GenerateCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Message type: INVOICE, DESADV, or ORDERSP")
    private MessageType messageType;
    
    @Option(names = {"-o", "--output"}, description = "Output file", required = true)
    private File outputFile;
    
    @Option(names = {"-f", "--from-order"}, description = "Generate from ORDER file")
    private File orderFile;
    
    @Option(names = {"--sender"}, description = "Sender party ID", defaultValue = "SENDER001")
    private String senderPartyId;
    
    @Option(names = {"--receiver"}, description = "Receiver party ID", defaultValue = "RECEIVER001")
    private String receiverPartyId;
    
    @Option(names = {"--format"}, description = "Output format: XML or JSON", defaultValue = "XML")
    private String format;
    
    private final CIIMessagingService service = new CIIMessagingServiceImpl();
    
    @Override
    public Integer call() throws Exception {
        System.out.println("Generating " + messageType + " message...");
        
        CIIMessage message;
        
        if (orderFile != null && orderFile.exists()) {
            // Generate from existing ORDER
            CIIMessage order = service.readMessage(orderFile);
            
            switch (messageType) {
                case INVOICE:
                    message = service.createInvoiceResponse(order);
                    break;
                case DESADV:
                    message = service.createDespatchAdvice(order);
                    break;
                case ORDERSP:
                    message = service.createOrderResponse(order, 
                        com.cii.messaging.service.OrderResponseType.ACCEPTED);
                    break;
                default:
                    System.err.println("Cannot generate " + messageType + " from ORDER");
                    return 1;
            }
        } else {
            // Generate sample message
            message = createSampleMessage();
        }
        
        // Write output
        if ("JSON".equalsIgnoreCase(format)) {
            String json = service.convertToJson(message);
            java.nio.file.Files.writeString(outputFile.toPath(), json,
                    java.nio.charset.StandardCharsets.UTF_8);
        } else {
            service.writeMessage(message, outputFile);
        }
        
        System.out.println("Generated " + messageType + " saved to: " + outputFile.getAbsolutePath());
        return 0;
    }
    
    private CIIMessage createSampleMessage() {
        return CIIMessage.builder()
                .messageId("MSG-" + System.currentTimeMillis())
                .messageType(messageType)
                .creationDateTime(LocalDateTime.now())
                .senderPartyId(senderPartyId)
                .receiverPartyId(receiverPartyId)
                .header(createSampleHeader())
                .lineItems(createSampleLineItems())
                .totals(createSampleTotals())
                .build();
    }
    
    private DocumentHeader createSampleHeader() {
        return DocumentHeader.builder()
                .documentNumber("DOC-" + System.currentTimeMillis())
                .documentDate(LocalDate.now())
                .currency("EUR")
                .buyerReference("BUY-REF-001")
                .sellerReference("SEL-REF-001")
                .paymentTerms(PaymentTerms.builder()
                        .description("30 days net")
                        .dueDate(LocalDate.now().plusDays(30))
                        .build())
                .build();
    }
    
    private java.util.List<LineItem> createSampleLineItems() {
        return Arrays.asList(
                LineItem.builder()
                        .lineNumber("1")
                        .productId("PROD001")
                        .description("Sample Product 1")
                        .quantity(new BigDecimal("10"))
                        .unitCode("EA")
                        .unitPrice(new BigDecimal("100.00"))
                        .lineAmount(new BigDecimal("1000.00"))
                        .taxRate(new BigDecimal("20"))
                        .taxCategory("S")
                        .build(),
                LineItem.builder()
                        .lineNumber("2")
                        .productId("PROD002")
                        .description("Sample Product 2")
                        .quantity(new BigDecimal("5"))
                        .unitCode("EA")
                        .unitPrice(new BigDecimal("50.00"))
                        .lineAmount(new BigDecimal("250.00"))
                        .taxRate(new BigDecimal("20"))
                        .taxCategory("S")
                        .build()
        );
    }
    
    private TotalsInformation createSampleTotals() {
        return TotalsInformation.builder()
                .lineTotalAmount(new BigDecimal("1250.00"))
                .taxBasisAmount(new BigDecimal("1250.00"))
                .taxTotalAmount(new BigDecimal("250.00"))
                .grandTotalAmount(new BigDecimal("1500.00"))
                .duePayableAmount(new BigDecimal("1500.00"))
                .build();
    }
}
