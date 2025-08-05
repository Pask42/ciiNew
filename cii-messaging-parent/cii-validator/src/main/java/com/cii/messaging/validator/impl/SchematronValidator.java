package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.validator.*;
import com.cii.messaging.writer.CIIWriter;
import com.cii.messaging.writer.CIIWriterFactory;
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

        try {
            if (schematronXslt == null) {
                return createErrorResult("Schematron rules not loaded");
            }

            XsltTransformer transformer = schematronXslt.load();
            transformer.setSource(new StreamSource(inputStream));
            XdmDestination destination = new XdmDestination();
            transformer.setDestination(destination);
            transformer.transform();

            parseSchematronResults(destination.getXdmNode(), resultBuilder);

            resultBuilder.validatedAgainst("Schematron EN 16931");
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
                    .build();
        }
    }
    
    @Override
    public void setSchemaVersion(SchemaVersion version) {
        this.schemaVersion = version;
        loadSchematronRules();
    }
    
    private void loadSchematronRules() {
        String resource = String.format("schematron/%s.xslt", schemaVersion.getVersion());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                logger.error("Schematron resource not found: {}", resource);
                schematronXslt = null;
                return;
            }
            logger.info("Loading Schematron rules from {}", resource);
            XsltCompiler compiler = processor.newXsltCompiler();
            schematronXslt = compiler.compile(new StreamSource(is));
        } catch (Exception e) {
            logger.error("Failed to load Schematron rules", e);
            schematronXslt = null;
        }
    }
    
    private void parseSchematronResults(XdmNode resultNode, ValidationResult.ValidationResultBuilder builder) {
        try {
            XPathCompiler xpathCompiler = processor.newXPathCompiler();
            xpathCompiler.declareNamespace("svrl", "http://purl.oclc.org/dsdl/svrl");

            net.sf.saxon.s9api.QName testQName = new net.sf.saxon.s9api.QName("test");
            net.sf.saxon.s9api.QName locationQName = new net.sf.saxon.s9api.QName("location");

            XPathSelector failedAssertions = xpathCompiler.compile("//svrl:failed-assert").load();
            failedAssertions.setContextItem(resultNode);

            List<ValidationError> errors = new ArrayList<>();
            for (XdmItem item : failedAssertions) {
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

            XPathSelector successfulReports = xpathCompiler.compile("//svrl:successful-report").load();
            successfulReports.setContextItem(resultNode);
            List<ValidationWarning> warnings = new ArrayList<>();
            for (XdmItem item : successfulReports) {
                XdmNode report = (XdmNode) item;
                String test = report.getAttributeValue(testQName);
                String location = report.getAttributeValue(locationQName);
                String text = getTextContent(report);

                ValidationWarning warning = ValidationWarning.builder()
                        .message(text)
                        .location(location)
                        .rule(test)
                        .build();
                warnings.add(warning);
            }

            builder.errors(errors);
            builder.warnings(warnings);
            builder.valid(errors.isEmpty());

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
