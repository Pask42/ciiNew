package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.reader.CIIReaderFactory;
import com.cii.messaging.validator.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

        // Example business rules
        if (message.getHeader() == null) {
            errors.add(ValidationError.builder()
                    .message("Document header is required")
                    .severity(ValidationError.ErrorSeverity.ERROR)
                    .rule("BR-01")
                    .build());
        }

        if (message.getLineItems() == null || message.getLineItems().isEmpty()) {
            warnings.add(ValidationWarning.builder()
                    .message("Document should contain at least one line item")
                    .rule("BR-02")
                    .build());
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
