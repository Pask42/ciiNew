package com.cii.messaging.validator.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.validator.ValidationError;
import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessRulesValidatorTest {

    private String readSample() throws IOException {
        return Files.readString(Path.of("src", "test", "resources", "invoice-sample.xml"));
    }

    @Test
    void validateStringValidXml() throws Exception {
        String xml = readSample();
        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(xml);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateInputStreamValidXml() throws Exception {
        BusinessRulesValidator validator = new BusinessRulesValidator();
        try (InputStream is = Files.newInputStream(Path.of("src", "test", "resources", "invoice-sample.xml"))) {
            ValidationResult result = validator.validate(is);
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }
    }

    @Test
    void validateStringInvalidXmlProducesFatalError() {
        String xml = "<invalid>";
        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(xml);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(ValidationError.ErrorSeverity.FATAL, result.getErrors().get(0).getSeverity());
    }

    @Test
    void validateInputStreamInvalidXmlProducesFatalError() {
        InputStream is = new ByteArrayInputStream("<invalid>".getBytes(StandardCharsets.UTF_8));
        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(is);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(ValidationError.ErrorSeverity.FATAL, result.getErrors().get(0).getSeverity());
    }

    @Test
    void validateMessageMissingRequiredReferences() {
        CIIMessage message = CIIMessage.builder()
                .header(DocumentHeader.builder()
                        .documentDate(LocalDate.now())
                        .build())
                .lineItems(List.of(LineItem.builder()
                        .lineNumber("1")
                        .quantity(new BigDecimal("1"))
                        .unitPrice(new BigDecimal("10"))
                        .lineAmount(new BigDecimal("10"))
                        .build()))
                .totals(TotalsInformation.builder()
                        .lineTotalAmount(new BigDecimal("10"))
                        .grandTotalAmount(new BigDecimal("10"))
                        .duePayableAmount(new BigDecimal("10"))
                        .build())
                .build();

        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(message);
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> "BR-03".equals(e.getRule())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "BR-05".equals(e.getRule())));
    }

    @Test
    void validateMessageAmountIncoherence() {
        CIIMessage message = CIIMessage.builder()
                .header(DocumentHeader.builder()
                        .documentNumber("INV-1")
                        .documentDate(LocalDate.now())
                        .buyerReference("BUY-1")
                        .build())
                .lineItems(List.of(LineItem.builder()
                        .lineNumber("1")
                        .quantity(new BigDecimal("2"))
                        .unitPrice(new BigDecimal("10"))
                        .lineAmount(new BigDecimal("15")) // should be 20
                        .build()))
                .totals(TotalsInformation.builder()
                        .lineTotalAmount(new BigDecimal("20")) // should be 15
                        .grandTotalAmount(new BigDecimal("20"))
                        .prepaidAmount(new BigDecimal("5"))
                        .duePayableAmount(new BigDecimal("10")) // should be 15
                        .build())
                .build();

        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(message);
        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> "BR-06".equals(e.getRule())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "BR-07".equals(e.getRule())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "BR-08".equals(e.getRule())));
    }

    @Test
    void validateMessageWithRoundingDifferences() {
        CIIMessage message = CIIMessage.builder()
                .header(DocumentHeader.builder()
                        .documentNumber("INV-2")
                        .documentDate(LocalDate.now())
                        .buyerReference("BUY-2")
                        .build())
                .lineItems(List.of(LineItem.builder()
                        .lineNumber("1")
                        .quantity(new BigDecimal("3"))
                        .unitPrice(new BigDecimal("6.86"))
                        .lineAmount(new BigDecimal("20.580000000000002"))
                        .build()))
                .totals(TotalsInformation.builder()
                        .lineTotalAmount(new BigDecimal("20.58"))
                        .grandTotalAmount(new BigDecimal("20.58"))
                        .duePayableAmount(new BigDecimal("20.58"))
                        .build())
                .build();

        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(message);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
}
