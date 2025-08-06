package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.validator.*;
import com.cii.messaging.writer.CIIWriter;
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.writer.CIIWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

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
        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            String xml = writer.writeToString(message);
            return validate(xml);
        } catch (Exception e) {
            ValidationError error = ValidationError.builder()
                    .message("Failed to serialize message: " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            List<ValidationError> errors = new ArrayList<>();
            errors.add(error);
            return ValidationResult.builder()
                    .valid(false)
                    .errors(errors)
                    .validatedAgainst("XSD " + schemaVersion.getVersion())
                    .build();
        }
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
            // Read the input so we can parse and validate with the same content
            byte[] xmlBytes = inputStream.readAllBytes();

            // Basic well-formedness and XXE protection
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new ByteArrayInputStream(xmlBytes));

            Schema schema = getSchemaForVersion(schemaVersion);
            Validator validator = schema.newValidator();
            try {
                validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
                logger.warn("Validator property not supported", e);
            }

            ValidationErrorHandler handler = new ValidationErrorHandler(errors, warnings);
            validator.setErrorHandler(handler);

            Source source = new StreamSource(new ByteArrayInputStream(xmlBytes));
            validator.validate(source);

            resultBuilder.valid(!handler.hasErrors());
            resultBuilder.errors(errors);
            resultBuilder.warnings(warnings);
            resultBuilder.validatedAgainst("XSD " + schemaVersion.getVersion());
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

    private Schema getSchemaForVersion(SchemaVersion version) throws SAXException, IOException {
        String key = version.getVersion();
        if (schemaCache.containsKey(key)) {
            return schemaCache.get(key);
        }

        String resourcePath = "/xsd/" + key.toLowerCase() + "/CrossIndustryInvoice.xsd";
        InputStream xsdStream = XSDValidator.class.getResourceAsStream(resourcePath);
        if (xsdStream == null) {
            logger.warn("XSD schema not available for version: {}", version.getVersion());
            throw new SAXException("XSD schema not found for version: " + version.getVersion());
        }
        try (InputStream is = xsdStream) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Schema schema = factory.newSchema(new StreamSource(is));
            schemaCache.put(key, schema);
            return schema;
        }
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
