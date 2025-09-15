package com.cii.messaging.validator.impl;

import com.cii.messaging.validator.*;
import com.cii.messaging.validator.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validator de messages CII basé exclusivement sur les schémas XSD D23B.
 * <p>
 * Cette implémentation charge les schémas officiels UN/CEFACT pour la version
 * D23B et exécute une validation XSD stricte pour chaque type de message
 * supporté. Aucune autre version n'est autorisée.
 * </p>
 */
public class XSDValidator implements CIIValidator {
    private static final Logger logger = LoggerFactory.getLogger(XSDValidator.class);
    private static final SchemaVersion SCHEMA_VERSION = SchemaVersion.D23B;

    private final Map<MessageType, Schema> schemaCache = new ConcurrentHashMap<>();

    @Override
    public ValidationResult validate(File xmlFile) {
        long start = System.currentTimeMillis();
        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();

        try (InputStream is = new FileInputStream(xmlFile)) {
            byte[] data = is.readAllBytes();
            MessageType type = detectMessageType(data);
            return performValidation(new ByteArrayInputStream(data), builder, start, type);
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

        try {
            byte[] data = inputStream.readAllBytes();
            MessageType type = detectMessageType(data);
            return performValidation(new ByteArrayInputStream(data), builder, start, type);
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

    /**
     * Seule la version D23B est supportée. Toute autre valeur provoque une
     * {@link IllegalArgumentException}.
     */
    @Override
    public void setSchemaVersion(SchemaVersion version) {
        if (version != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Seule la version D23B est supportée");
        }
    }

    private ValidationResult performValidation(InputStream inputStream,
                                               ValidationResult.ValidationResultBuilder builder,
                                               long start,
                                               MessageType type) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        try {
            Schema schema = getSchema(type);
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
            builder.validatedAgainst("XSD " + SCHEMA_VERSION.getVersion());
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
            builder.validatedAgainst("XSD " + SCHEMA_VERSION.getVersion());
            return builder.build();
        }
    }

    private Schema getSchema(MessageType type) throws SAXException, IOException {
        Schema cached = schemaCache.get(type);
        if (cached != null) {
            return cached;
        }

        String resource;
        switch (type) {
            case INVOICE -> resource = "xsd/CrossIndustryInvoice.xsd";
            case DESADV -> resource = "xsd/CrossIndustryDespatchAdvice.xsd";
            case ORDER -> resource = "xsd/CrossIndustryOrder.xsd";
            case ORDERSP -> resource = "xsd/CrossIndustryOrderResponse.xsd";
            default -> throw new IllegalArgumentException("Type de message non pris en charge : " + type);
        }

        Schema schema;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema();
            } else {
                schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                        .newSchema(new StreamSource(is));
            }
        }
        Schema existing = schemaCache.putIfAbsent(type, schema);
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
            switch (root) {
                case "CrossIndustryInvoice":
                    return MessageType.INVOICE;
                case "CrossIndustryDespatchAdvice":
                    return MessageType.DESADV;
                case "CrossIndustryOrder":
                    return MessageType.ORDER;
                case "CrossIndustryOrderResponse":
                    return MessageType.ORDERSP;
                default:
                    throw new SAXException("Élément racine inconnu : " + root);
            }
        }
    }

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

