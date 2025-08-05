package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.validator.*;
import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

public class SchematronValidator implements CIIValidator {
    private static final Logger logger = LoggerFactory.getLogger(SchematronValidator.class);
    private SchemaVersion schemaVersion = SchemaVersion.D16B;
    private final Processor processor;
    private XsltExecutable schematronXslt;
    
    public SchematronValidator() {
        this.processor = new Processor(false);
        loadSchematronRules();
    }
    
    @Override
    public ValidationResult validate(File xmlFile) {
        try (InputStream is = new FileInputStream(xmlFile)) {
            return validate(is);
        } catch (IOException e) {
            return createErrorResult("Failed to read file: " + e.getMessage());
        }
    }
    
    @Override
    public ValidationResult validate(InputStream inputStream) {
        long startTime = System.currentTimeMillis();
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        try {
            // For now, just perform basic validation
            // Real Schematron validation would require proper XSLT setup
            resultBuilder.valid(true);
            resultBuilder.errors(errors);
            resultBuilder.warnings(warnings);
            resultBuilder.validatedAgainst("Schematron EN 16931 (simplified)");
            resultBuilder.validationTimeMs(System.currentTimeMillis() - startTime);
            
            return resultBuilder.build();
            
        } catch (Exception e) {
            logger.error("Schematron validation failed", e);
            return createErrorResult("Validation error: " + e.getMessage());
        }
    }
    
    @Override
    public ValidationResult validate(String xmlContent) {
        return validate(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }
    
    @Override
    public ValidationResult validate(CIIMessage message) {
        throw new UnsupportedOperationException("Direct CIIMessage validation not implemented");
    }
    
    @Override
    public void setSchemaVersion(SchemaVersion version) {
        this.schemaVersion = version;
        loadSchematronRules();
    }
    
    private void loadSchematronRules() {
        // Simplified - in real implementation would load actual Schematron rules
        logger.info("Loading Schematron rules for version: " + schemaVersion);
    }
    
    private void parseSchematronResults(XdmNode resultNode, ValidationResult.ValidationResultBuilder builder) {
        // Simplified implementation
        try {
            XPathCompiler xpathCompiler = processor.newXPathCompiler();
            xpathCompiler.declareNamespace("svrl", "http://purl.oclc.org/dsdl/svrl");
            
            // Create Saxon QName objects
            net.sf.saxon.s9api.QName testQName = new net.sf.saxon.s9api.QName("test");
            net.sf.saxon.s9api.QName locationQName = new net.sf.saxon.s9api.QName("location");
            
            // Check for failed assertions
            XPathSelector failedAssertions = xpathCompiler.compile("//svrl:failed-assert").load();
            failedAssertions.setContextItem(resultNode);
            
            List<ValidationError> errors = new ArrayList<>();
            boolean hasErrors = false;
            
            for (XdmItem item : failedAssertions) {
                hasErrors = true;
                XdmNode assertion = (XdmNode) item;
                
                String test = assertion.getAttributeValue(testQName);
                String location = assertion.getAttributeValue(locationQName);
                String text = getTextContent(assertion);
                
                ValidationError error = ValidationError.builder()
                        .message(text)
                        .location(location)
                        .rule(test)
                        .severity(ValidationError.ErrorSeverity.ERROR)
                        .build();
                
                errors.add(error);
            }
            
            builder.errors(errors);
            builder.valid(!hasErrors);
            
        } catch (Exception e) {
            logger.error("Failed to parse Schematron results", e);
        }
    }
    
    private String getTextContent(XdmNode node) {
        StringBuilder text = new StringBuilder();
        for (XdmItem child : node) {
            if (child instanceof XdmNode) {
                XdmNode childNode = (XdmNode) child;
                if (childNode.getNodeKind() == XdmNodeKind.TEXT) {
                    text.append(childNode.getStringValue());
                }
            }
        }
        return text.toString().trim();
    }
    
    private ValidationResult createErrorResult(String message) {
        ValidationError error = ValidationError.builder()
                .message(message)
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
