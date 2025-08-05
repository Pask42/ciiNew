package com.cii.messaging.validator.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.reader.CIIReaderFactory;
import com.cii.messaging.validator.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BusinessRulesValidator implements CIIValidator {
    private SchemaVersion schemaVersion = SchemaVersion.D16B;

    @Override
    public ValidationResult validate(File xmlFile) {
        // Implement business rules validation
        return ValidationResult.builder()
                .valid(true)
                .validatedAgainst("Business Rules")
                .build();
    }

    @Override
    public ValidationResult validate(InputStream inputStream) {
        try {
            String xmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return validate(xmlContent);
        } catch (IOException e) {
            return buildFatalResult("Failed to read XML from input stream: " + e.getMessage());
        }
    }

    @Override
    public ValidationResult validate(String xmlContent) {
        try {
            CIIReader reader = CIIReaderFactory.createReader(xmlContent);
            CIIMessage message = reader.read(xmlContent);
            return validate(message);
        } catch (Exception e) {
            return buildFatalResult("Failed to parse XML: " + e.getMessage());
        }
    }

    private ValidationResult buildFatalResult(String message) {
        ValidationError error = ValidationError.builder()
                .message(message)
                .severity(ValidationError.ErrorSeverity.FATAL)
                .build();
        List<ValidationError> errors = new ArrayList<>();
        errors.add(error);
        return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .validatedAgainst("Business Rules")
                .build();
    }

    @Override
    public ValidationResult validate(CIIMessage message) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        // Basic checks on the document structure
        if (message.getHeader() == null) {
            errors.add(ValidationError.builder()
                    .message("Document header is required")
                    .severity(ValidationError.ErrorSeverity.ERROR)
                    .rule("BR-01")
                    .build());
        } else {
            DocumentHeader header = message.getHeader();
            if (header.getDocumentNumber() == null || header.getDocumentNumber().isBlank()) {
                errors.add(ValidationError.builder()
                        .message("Document number is mandatory")
                        .severity(ValidationError.ErrorSeverity.ERROR)
                        .rule("BR-03")
                        .build());
            }
            if (header.getDocumentDate() == null) {
                errors.add(ValidationError.builder()
                        .message("Document date is mandatory")
                        .severity(ValidationError.ErrorSeverity.ERROR)
                        .rule("BR-04")
                        .build());
            }
            if (header.getBuyerReference() == null || header.getBuyerReference().isBlank()) {
                errors.add(ValidationError.builder()
                        .message("Buyer reference is mandatory")
                        .severity(ValidationError.ErrorSeverity.ERROR)
                        .rule("BR-05")
                        .build());
            }
        }

        if (message.getLineItems() == null || message.getLineItems().isEmpty()) {
            warnings.add(ValidationWarning.builder()
                    .message("Document should contain at least one line item")
                    .rule("BR-02")
                    .build());
        } else {
            for (LineItem item : message.getLineItems()) {
                if (item.getQuantity() != null && item.getUnitPrice() != null && item.getLineAmount() != null) {
                    BigDecimal expected = item.getQuantity().multiply(item.getUnitPrice());
                    if (expected.compareTo(item.getLineAmount()) != 0) {
                        errors.add(ValidationError.builder()
                                .message("Line amount must equal quantity multiplied by unit price")
                                .severity(ValidationError.ErrorSeverity.ERROR)
                                .rule("BR-06")
                                .build());
                    }
                }
            }
        }

        TotalsInformation totals = message.getTotals();
        if (totals != null && message.getLineItems() != null) {
            BigDecimal sumLines = message.getLineItems().stream()
                    .map(LineItem::getLineAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totals.getLineTotalAmount() != null && sumLines.compareTo(totals.getLineTotalAmount()) != 0) {
                errors.add(ValidationError.builder()
                        .message("Line total amount must equal sum of line item amounts")
                        .severity(ValidationError.ErrorSeverity.ERROR)
                        .rule("BR-07")
                        .build());
            }

            if (totals.getGrandTotalAmount() != null && totals.getDuePayableAmount() != null) {
                BigDecimal expectedDue = totals.getGrandTotalAmount();
                if (totals.getPrepaidAmount() != null) {
                    expectedDue = expectedDue.subtract(totals.getPrepaidAmount());
                }
                if (expectedDue.compareTo(totals.getDuePayableAmount()) != 0) {
                    errors.add(ValidationError.builder()
                            .message("Due payable amount must equal grand total minus prepaid amount")
                            .severity(ValidationError.ErrorSeverity.ERROR)
                            .rule("BR-08")
                            .build());
                }
            }
        }

        resultBuilder.valid(errors.isEmpty());
        resultBuilder.errors(errors);
        resultBuilder.warnings(warnings);
        resultBuilder.validatedAgainst("Business Rules");

        return resultBuilder.build();
    }

    @Override
    public void setSchemaVersion(SchemaVersion version) {
        this.schemaVersion = version;
    }
}
