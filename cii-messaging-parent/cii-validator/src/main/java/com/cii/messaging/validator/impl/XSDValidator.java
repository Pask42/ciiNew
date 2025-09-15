package com.cii.messaging.validator.impl;

import com.cii.messaging.validator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validator de messages CII basé sur les schémas XSD officiels UN/CEFACT.
 * <p>
 * Les schémas sont chargés dynamiquement selon la version configurée via
 * {@link SchemaVersion}.
 * </p>
 */
public class XSDValidator implements CIIValidator {
    private static final Logger logger = LoggerFactory.getLogger(XSDValidator.class);
    private final Map<SchemaCacheKey, Schema> schemaCache = new ConcurrentHashMap<>();
    private volatile SchemaVersion schemaVersion = SchemaVersion.getDefault();

    @Override
    public ValidationResult validate(File xmlFile) {
        long start = System.currentTimeMillis();
        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();

        SchemaVersion currentVersion = this.schemaVersion;
        try (InputStream is = new FileInputStream(xmlFile)) {
            byte[] data = is.readAllBytes();
            MessageType type = detectMessageType(data);
            return performValidation(new ByteArrayInputStream(data), builder, start, type, currentVersion);
        } catch (Exception e) {
            builder.valid(false);
            builder.validationTimeMs(System.currentTimeMillis() - start);
            ValidationError error = ValidationError.builder()
                    .message("Échec de la validation du fichier : " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            builder.errors(List.of(error));
            return builder.build();
        }
    }

    @Override
    public ValidationResult validate(InputStream inputStream) {
        long start = System.currentTimeMillis();
        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();

        SchemaVersion currentVersion = this.schemaVersion;
        try {
            byte[] data = inputStream.readAllBytes();
            MessageType type = detectMessageType(data);
            return performValidation(new ByteArrayInputStream(data), builder, start, type, currentVersion);
        } catch (Exception e) {
            builder.valid(false);
            builder.validationTimeMs(System.currentTimeMillis() - start);
            ValidationError error = ValidationError.builder()
                    .message("Échec de la validation du flux : " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            builder.errors(List.of(error));
            return builder.build();
        }
    }

    @Override
    public ValidationResult validate(String xmlContent) {
        return validate(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void setSchemaVersion(SchemaVersion version) {
        this.schemaVersion = Objects.requireNonNull(version, "version");
        schemaCache.clear();
    }

    private ValidationResult performValidation(InputStream inputStream,
                                               ValidationResult.ValidationResultBuilder builder,
                                               long start,
                                               MessageType type,
                                               SchemaVersion version) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        try {
            Schema schema = getSchema(type, version);
            Validator validator = schema.newValidator();
            ValidationErrorHandler handler = new ValidationErrorHandler(errors, warnings);
            validator.setErrorHandler(handler);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);

            XMLReader reader = factory.newSAXParser().getXMLReader();
            Source source = new SAXSource(reader, new InputSource(new BufferedInputStream(inputStream)));
            validator.validate(source);

            builder.valid(!handler.hasErrors());
            builder.errors(errors);
            builder.warnings(warnings);
            builder.validatedAgainst("XSD " + version.getVersion());
            builder.validationTimeMs(System.currentTimeMillis() - start);
            return builder.build();
        } catch (Exception e) {
            logger.error("Échec de la validation", e);
            builder.valid(false);
            builder.validationTimeMs(System.currentTimeMillis() - start);
            ValidationError error = ValidationError.builder()
                    .message("Erreur de validation : " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            errors.add(error);
            builder.errors(errors);
            builder.warnings(warnings);
            builder.validatedAgainst("XSD " + version.getVersion());
            return builder.build();
        }
    }

    private Schema getSchema(MessageType type, SchemaVersion version) throws SAXException, IOException {
        SchemaCacheKey cacheKey = new SchemaCacheKey(type, version);
        Schema cached = schemaCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String xsdName = switch (type) {
            case INVOICE -> "CrossIndustryInvoice.xsd";
            case DESPATCH_ADVICE -> "CrossIndustryDespatchAdvice.xsd";
            case ORDER -> "CrossIndustryOrder.xsd";
            case ORDER_RESPONSE -> "CrossIndustryOrderResponse.xsd";
        };

        Schema schema = UneceSchemaLoader.loadSchema(xsdName, version);
        Schema existing = schemaCache.putIfAbsent(cacheKey, schema);
        return existing != null ? existing : schema;
    }

    private MessageType detectMessageType(byte[] data) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            Document doc = builder.parse(bais);
            String root = doc.getDocumentElement().getLocalName();
            try {
                return MessageType.fromRootElement(root);
            } catch (IllegalArgumentException ex) {
                throw new SAXException("Élément racine inconnu : " + root, ex);
            }
        }
    }

    private record SchemaCacheKey(MessageType type, SchemaVersion version) { }

    private static class ValidationErrorHandler implements ErrorHandler {
        private final List<ValidationError> errors;
        private final List<ValidationWarning> warnings;
        private boolean hasErrors = false;

        ValidationErrorHandler(List<ValidationError> errors, List<ValidationWarning> warnings) {
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

        boolean hasErrors() {
            return hasErrors;
        }
    }
}

