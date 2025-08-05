package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.validator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XSDValidator implements CIIValidator {
    private static final Logger logger = LoggerFactory.getLogger(XSDValidator.class);
    private SchemaVersion schemaVersion = SchemaVersion.D16B;
    private final Map<String, Schema> schemaCache = new HashMap<>();
    
    @Override
    public ValidationResult validate(File xmlFile) {
        long startTime = System.currentTimeMillis();
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        
        try (InputStream is = new FileInputStream(xmlFile)) {
            return performValidation(is, resultBuilder, startTime);
        } catch (Exception e) {
            resultBuilder.valid(false);
            resultBuilder.validationTimeMs(System.currentTimeMillis() - startTime);
            ValidationError error = ValidationError.builder()
                    .message("Failed to validate file: " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            List<ValidationError> errors = new ArrayList<>();
            errors.add(error);
            resultBuilder.errors(errors);
            return resultBuilder.build();
        }
    }
    
    @Override
    public ValidationResult validate(InputStream inputStream) {
        long startTime = System.currentTimeMillis();
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        return performValidation(inputStream, resultBuilder, startTime);
    }
    
    @Override
    public ValidationResult validate(String xmlContent) {
        return validate(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }
    
    @Override
    public ValidationResult validate(CIIMessage message) {
        // This would require writing the message to XML first
        throw new UnsupportedOperationException("Direct CIIMessage validation not yet implemented");
    }
    
    @Override
    public void setSchemaVersion(SchemaVersion version) {
        this.schemaVersion = version;
    }
    
    private ValidationResult performValidation(InputStream inputStream, 
                                              ValidationResult.ValidationResultBuilder resultBuilder,
                                              long startTime) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        try {
            // For now, perform basic XML validation
            // Real XSD validation would require loading actual XSD files
            
            // Simple well-formedness check
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(inputStream);
            
            resultBuilder.valid(true);
            resultBuilder.errors(errors);
            resultBuilder.warnings(warnings);
            resultBuilder.validatedAgainst("XSD " + schemaVersion.getVersion() + " (simplified)");
            resultBuilder.validationTimeMs(System.currentTimeMillis() - startTime);
            
            return resultBuilder.build();
            
        } catch (Exception e) {
            logger.error("Validation failed", e);
            resultBuilder.valid(false);
            resultBuilder.validationTimeMs(System.currentTimeMillis() - startTime);
            ValidationError error = ValidationError.builder()
                    .message("Validation error: " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            errors.add(error);
            resultBuilder.errors(errors);
            resultBuilder.warnings(warnings);
            return resultBuilder.build();
        }
    }
    
    private Schema getSchemaForVersion(SchemaVersion version) throws SAXException {
        // Simplified - would load real XSD in production
        String key = version.getVersion();
        if (!schemaCache.containsKey(key)) {
            logger.warn("XSD schema not available for version: " + version.getVersion());
        }
        return null;
    }
    
    private static class ValidationErrorHandler implements ErrorHandler {
        private final List<ValidationError> errors;
        private final List<ValidationWarning> warnings;
        private boolean hasErrors = false;
        
        public ValidationErrorHandler(List<ValidationError> errors, List<ValidationWarning> warnings) {
            this.errors = errors;
            this.warnings = warnings;
        }
        
        @Override
        public void warning(SAXParseException e) {
            ValidationWarning warning = ValidationWarning.builder()
                    .message(e.getMessage())
                    .location("Line " + e.getLineNumber() + ", Column " + e.getColumnNumber())
                    .build();
            warnings.add(warning);
        }
        
        @Override
        public void error(SAXParseException e) {
            hasErrors = true;
            ValidationError error = ValidationError.builder()
                    .message(e.getMessage())
                    .lineNumber(e.getLineNumber())
                    .columnNumber(e.getColumnNumber())
                    .severity(ValidationError.ErrorSeverity.ERROR)
                    .build();
            errors.add(error);
        }
        
        @Override
        public void fatalError(SAXParseException e) {
            hasErrors = true;
            ValidationError error = ValidationError.builder()
                    .message(e.getMessage())
                    .lineNumber(e.getLineNumber())
                    .columnNumber(e.getColumnNumber())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            errors.add(error);
        }
        
        public boolean hasErrors() {
            return hasErrors;
        }
    }
}
