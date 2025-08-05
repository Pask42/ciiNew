package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.validator.*;

import java.io.File;
import java.io.InputStream;
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
        // Similar implementation to validate(File)
        throw new UnsupportedOperationException("Stream validation not implemented for composite validator");
    }
    
    @Override
    public ValidationResult validate(String xmlContent) {
        // Similar implementation
        throw new UnsupportedOperationException("String validation not implemented for composite validator");
    }
    
    @Override
    public ValidationResult validate(CIIMessage message) {
        throw new UnsupportedOperationException("Message validation not implemented for composite validator");
    }
    
    @Override
    public void setSchemaVersion(SchemaVersion version) {
        this.schemaVersion = version;
        validators.forEach(v -> v.setSchemaVersion(version));
    }
}
