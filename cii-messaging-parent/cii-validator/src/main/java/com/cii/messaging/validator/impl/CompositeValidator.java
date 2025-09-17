package com.cii.messaging.validator.impl;

import com.cii.messaging.validator.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Chains multiple {@link CIIValidator} implementations and aggregates their results.
 */
public class CompositeValidator implements CIIValidator {
    private final List<CIIValidator> validators = new ArrayList<>();
    private SchemaVersion schemaVersion = SchemaVersion.getDefault();

    public CompositeValidator() {
        validators.add(new XSDValidator());
        validators.add(new SchematronValidator());
        validators.forEach(v -> v.setSchemaVersion(schemaVersion));
    }

    public void addValidator(CIIValidator validator) {
        validators.add(Objects.requireNonNull(validator, "validator"));
        validator.setSchemaVersion(schemaVersion);
    }

    @Override
    public ValidationResult validate(File xmlFile) {
        Objects.requireNonNull(xmlFile, "xmlFile");
        long start = System.currentTimeMillis();

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

            if (result.getErrors() != null) {
                allErrors.addAll(result.getErrors());
            }
            if (result.getWarnings() != null) {
                allWarnings.addAll(result.getWarnings());
            }

            String against = result.getValidatedAgainst();
            if (against != null && !against.isBlank()) {
                if (validatedAgainst.length() > 0) {
                    validatedAgainst.append(", ");
                }
                validatedAgainst.append(against);
            }
        }
        
        combinedResult.errors(allErrors);
        combinedResult.warnings(allWarnings);
        combinedResult.validatedAgainst(validatedAgainst.length() == 0 ? null : validatedAgainst.toString());
        combinedResult.validationTimeMs(System.currentTimeMillis() - start);

        return combinedResult.build();
    }
    
    @Override
    public ValidationResult validate(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try {
            byte[] data = inputStream.readAllBytes();
            return validateBuffered(data);
        } catch (IOException e) {
            ValidationError error = ValidationError.builder()
                    .message("Échec de la lecture du flux d'entrée : " + e.getMessage())
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
        Objects.requireNonNull(xmlContent, "xmlContent");
        byte[] data = xmlContent.getBytes(StandardCharsets.UTF_8);
        return validateBuffered(data);
    }
    
    @Override
    public void setSchemaVersion(SchemaVersion version) {
        this.schemaVersion = version;
        validators.forEach(v -> v.setSchemaVersion(version));
    }

    private ValidationResult validateBuffered(byte[] data) {
        long start = System.currentTimeMillis();

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

            if (result.getErrors() != null) {
                allErrors.addAll(result.getErrors());
            }
            if (result.getWarnings() != null) {
                allWarnings.addAll(result.getWarnings());
            }

            String against = result.getValidatedAgainst();
            if (against != null && !against.isBlank()) {
                if (validatedAgainst.length() > 0) {
                    validatedAgainst.append(", ");
                }
                validatedAgainst.append(against);
            }
        }

        combinedResult.errors(allErrors);
        combinedResult.warnings(allWarnings);
        combinedResult.validatedAgainst(validatedAgainst.length() == 0 ? null : validatedAgainst.toString());
        combinedResult.validationTimeMs(System.currentTimeMillis() - start);

        return combinedResult.build();
    }
}
