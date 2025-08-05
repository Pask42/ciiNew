package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.validator.*;
import com.cii.messaging.writer.CIIWriter;
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.writer.CIIWriterFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CompositeValidator implements CIIValidator {
    private final List<CIIValidator> validators = new ArrayList<>();
    private SchemaVersion schemaVersion = SchemaVersion.D16B;
    
    public CompositeValidator() {
        validators.add(new XSDValidator());
        validators.add(new SchematronValidator());
        validators.add(new BusinessRulesValidator());
    }
    
    public void addValidator(CIIValidator validator) {
        validators.add(validator);
    }
    
    @Override
    public ValidationResult validate(File xmlFile) {
        ValidationResult.ValidationResultBuilder combinedResult = ValidationResult.builder();
        combinedResult.valid(true);
        
        List<ValidationError> allErrors = new ArrayList<>();
        List<ValidationWarning> allWarnings = new ArrayList<>();
        StringBuilder validatedAgainst = new StringBuilder();
        
        for (CIIValidator validator : validators) {
            ValidationResult result = validator.validate(xmlFile);
            
            if (!result.isValid()) {
                combinedResult.valid(false);
            }
            
            allErrors.addAll(result.getErrors());
            allWarnings.addAll(result.getWarnings());
            
            if (validatedAgainst.length() > 0) {
                validatedAgainst.append(", ");
            }
            validatedAgainst.append(result.getValidatedAgainst());
        }
        
        combinedResult.errors(allErrors);
        combinedResult.warnings(allWarnings);
        combinedResult.validatedAgainst(validatedAgainst.toString());
        
        return combinedResult.build();
    }
    
    @Override
    public ValidationResult validate(InputStream inputStream) {
        try {
            byte[] data = inputStream.readAllBytes();
            return validateBuffered(data);
        } catch (IOException e) {
            ValidationError error = ValidationError.builder()
                    .message("Failed to read input stream: " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            List<ValidationError> errors = new ArrayList<>();
            errors.add(error);
            return ValidationResult.builder()
                    .valid(false)
                    .errors(errors)
                    .build();
        }
    }

    @Override
    public ValidationResult validate(String xmlContent) {
        byte[] data = xmlContent.getBytes(StandardCharsets.UTF_8);
        return validateBuffered(data);
    }

    @Override
    public ValidationResult validate(CIIMessage message) {
        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            String xml = writer.writeToString(message);
            return validate(xml);
        } catch (CIIWriterException e) {
            ValidationError error = ValidationError.builder()
                    .message("Failed to serialize message: " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            List<ValidationError> errors = new ArrayList<>();
            errors.add(error);
            return ValidationResult.builder()
                    .valid(false)
                    .errors(errors)
                    .build();
        }
    }
    
    @Override
    public void setSchemaVersion(SchemaVersion version) {
        this.schemaVersion = version;
        validators.forEach(v -> v.setSchemaVersion(version));
    }

    private ValidationResult validateBuffered(byte[] data) {
        ValidationResult.ValidationResultBuilder combinedResult = ValidationResult.builder();
        combinedResult.valid(true);

        List<ValidationError> allErrors = new ArrayList<>();
        List<ValidationWarning> allWarnings = new ArrayList<>();
        StringBuilder validatedAgainst = new StringBuilder();

        for (CIIValidator validator : validators) {
            ValidationResult result = validator.validate(new ByteArrayInputStream(data));

            if (!result.isValid()) {
                combinedResult.valid(false);
            }

            allErrors.addAll(result.getErrors());
            allWarnings.addAll(result.getWarnings());

            if (validatedAgainst.length() > 0) {
                validatedAgainst.append(", ");
            }
            validatedAgainst.append(result.getValidatedAgainst());
        }

        combinedResult.errors(allErrors);
        combinedResult.warnings(allWarnings);
        combinedResult.validatedAgainst(validatedAgainst.toString());

        return combinedResult.build();
    }
}
