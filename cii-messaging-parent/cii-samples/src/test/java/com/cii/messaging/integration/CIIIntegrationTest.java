package com.cii.messaging.integration;

import com.cii.messaging.model.*;
import com.cii.messaging.service.*;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;
import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.api.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CIIIntegrationTest {
    
    private static CIIMessagingService service;
    private static Path tempDir;
    
    @BeforeAll
    static void setup() throws Exception {
        service = new CIIMessagingServiceImpl();
        tempDir = Files.createTempDirectory("cii-test");
    }
    
    @AfterAll
    static void cleanup() throws Exception {
        Files.walk(tempDir)
            .sorted(java.util.Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }
    
    @Test
    @Order(1)
    @DisplayName("Generate and validate INVOICE message")
    void testGenerateAndValidateInvoice() throws Exception {
        // Create invoice message
        CIIMessage invoice = createSampleInvoice();
        
        // Write to file
        File invoiceFile = tempDir.resolve("test-invoice.xml").toFile();
        service.writeMessage(invoice, invoiceFile);
        
        assertThat(invoiceFile).exists();
        
        // Validate the generated invoice
        ValidationResult result = service.validateMessage(invoiceFile);
        assertThat(result.isValid()).isTrue();
        
        // Read back and verify
        CIIMessage readInvoice = service.readMessage(invoiceFile);
        assertThat(readInvoice.getMessageType()).isEqualTo(MessageType.INVOICE);
        assertThat(readInvoice.getMessageId()).isEqualTo(invoice.getMessageId());
    }
    
    @Test
    @Order(2)
    @DisplayName("Convert ORDER to INVOICE")
    void testOrderToInvoiceConversion() throws Exception {
        // Read sample order
        File orderFile = new File("src/main/resources/samples/order-sample.xml");
        assumeTrue(orderFile.exists(), "Sample order file should exist");
        
        CIIMessage order = service.readMessage(orderFile);
        assertThat(order.getMessageType()).isEqualTo(MessageType.ORDER);
        
        // Convert to invoice
        CIIMessage invoice = service.createInvoiceResponse(order);
        
        assertThat(invoice.getMessageType()).isEqualTo(MessageType.INVOICE);
        assertThat(invoice.getSenderPartyId()).isEqualTo(order.getReceiverPartyId());
        assertThat(invoice.getReceiverPartyId()).isEqualTo(order.getSenderPartyId());
    }
    
    @Test
    @Order(3)
    @DisplayName("JSON conversion round-trip")
    void testJsonConversion() throws Exception {
        CIIMessage original = createSampleInvoice();
        
        // Convert to JSON
        String json = service.convertToJson(original);
        assertThat(json).isNotEmpty();
        assertThat(json).contains("\"messageType\":\"INVOICE\"");
        
        // Convert back to object
        CIIMessage restored = service.convertFromJson(json, MessageType.INVOICE);
        
        assertThat(restored.getMessageId()).isEqualTo(original.getMessageId());
        assertThat(restored.getMessageType()).isEqualTo(original.getMessageType());
        assertThat(restored.getLineItems()).hasSameSizeAs(original.getLineItems());
    }
    
    @Test
    @Order(4)
    @DisplayName("Create DESADV from ORDER")
    void testCreateDesadvFromOrder() throws Exception {
        CIIMessage order = createSampleOrder();
        
        CIIMessage desadv = service.createDespatchAdvice(order);
        
        assertThat(desadv.getMessageType()).isEqualTo(MessageType.DESADV);
        assertThat(desadv.getLineItems()).hasSameSizeAs(order.getLineItems());
    }
    
    @Test
    @Order(5)
    @DisplayName("Validation with errors")
    void testValidationWithErrors() throws Exception {
        String invalidXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:CrossIndustryInvoice xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100">
                <rsm:ExchangedDocument>
                    <!-- Missing required elements -->
                </rsm:ExchangedDocument>
            </rsm:CrossIndustryInvoice>
            """;

        File invalidFile = tempDir.resolve("invalid.xml").toFile();
        Files.writeString(invalidFile.toPath(), invalidXml, StandardCharsets.UTF_8);

        ValidationResult result = service.validateMessage(invalidFile);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Handle non-ASCII characters")
    void testNonAsciiCharacters() throws Exception {
        CIIMessage original = createSampleInvoice();
        original.getLineItems().get(0).setDescription("Täst Prödüct – Größe");

        File xmlFile = tempDir.resolve("unicode.xml").toFile();
        service.writeMessage(original, xmlFile);
        CIIMessage read = service.readMessage(xmlFile);
        assertThat(read.getLineItems().get(0).getDescription())
                .isEqualTo("Täst Prödüct – Größe");

        String json = service.convertToJson(original);
        File jsonFile = tempDir.resolve("unicode.json").toFile();
        Files.writeString(jsonFile.toPath(), json, StandardCharsets.UTF_8);
        String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
        CIIMessage fromJson = service.convertFromJson(jsonContent, MessageType.INVOICE);
        assertThat(fromJson.getLineItems().get(0).getDescription())
                .isEqualTo("Täst Prödüct – Größe");
    }
    
    private CIIMessage createSampleInvoice() {
        return CIIMessage.builder()
                .messageId("INV-TEST-001")
                .messageType(MessageType.INVOICE)
                .creationDateTime(LocalDateTime.now())
                .senderPartyId("SELLER001")
                .receiverPartyId("BUYER001")
                .header(DocumentHeader.builder()
                        .documentNumber("INV-2024-001")
                        .documentDate(LocalDate.now())
                        .currency("EUR")
                        .paymentTerms(PaymentTerms.builder()
                                .description("30 days net")
                                .dueDate(LocalDate.now().plusDays(30))
                                .build())
                        .build())
                .lineItems(Arrays.asList(
                        LineItem.builder()
                                .lineNumber("1")
                                .productId("PROD001")
                                .description("Test Product")
                                .quantity(new BigDecimal("10"))
                                .unitCode("EA")
                                .unitPrice(new BigDecimal("100.00"))
                                .lineAmount(new BigDecimal("1000.00"))
                                .taxRate(new BigDecimal("20"))
                                .build()
                ))
                .totals(TotalsInformation.builder()
                        .lineTotalAmount(new BigDecimal("1000.00"))
                        .taxTotalAmount(new BigDecimal("200.00"))
                        .grandTotalAmount(new BigDecimal("1200.00"))
                        .build())
                .build();
    }
    
    private CIIMessage createSampleOrder() {
        CIIMessage order = createSampleInvoice();
        order.setMessageType(MessageType.ORDER);
        order.setMessageId("ORD-TEST-001");
        return order;
    }
}
