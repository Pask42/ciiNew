package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.validator.*;
import com.cii.messaging.writer.CIIWriter;
import com.cii.messaging.writer.CIIWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XSDValidator implements CIIValidator {
    private static final Logger logger = LoggerFactory.getLogger(XSDValidator.class);
    private SchemaVersion schemaVersion = SchemaVersion.getDefault();
    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();
    
    @Override
    public ValidationResult validate(File xmlFile) {
        long startTime = System.currentTimeMillis();
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        
        try (InputStream is = new FileInputStream(xmlFile)) {
            byte[] data = is.readAllBytes();
            MessageType type = detectMessageType(data);
            return performValidation(new ByteArrayInputStream(data), resultBuilder, startTime, type);
        } catch (Exception e) {
            resultBuilder.valid(false);
            resultBuilder.validationTimeMs(System.currentTimeMillis() - startTime);
            ValidationError error = ValidationError.builder()
                    .message("Échec de la validation du fichier : " + e.getMessage())
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
        try {
            byte[] data = inputStream.readAllBytes();
            MessageType type = detectMessageType(data);
            return performValidation(new ByteArrayInputStream(data), resultBuilder, startTime, type);
        } catch (Exception e) {
            resultBuilder.valid(false);
            resultBuilder.validationTimeMs(System.currentTimeMillis() - startTime);
            ValidationError error = ValidationError.builder()
                    .message("Échec de la validation du flux : " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            List<ValidationError> errors = new ArrayList<>();
            errors.add(error);
            resultBuilder.errors(errors);
            return resultBuilder.build();
        }
    }
    
    @Override
    public ValidationResult validate(String xmlContent) {
        return validate(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }
    
    @Override
    public ValidationResult validate(CIIMessage message) {
        if (message.getBuyer() == null || message.getSeller() == null ||
                message.getLineItems() == null || message.getLineItems().isEmpty()) {
            ValidationError error = ValidationError.builder()
                    .message("Contenu du message requis manquant")
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

        try {
            CIIWriter writer = CIIWriterFactory.createWriter(message.getMessageType());
            String xml = writer.writeToString(message);
            return validate(xml);
        } catch (Exception e) {
            ValidationError error = ValidationError.builder()
                    .message("Échec de la sérialisation du message : " + e.getMessage())
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
                                              long startTime,
                                              MessageType messageType) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        try {
            Schema schema = getSchemaForVersion(schemaVersion, messageType);
            Validator validator = schema.newValidator();

            ValidationErrorHandler handler = new ValidationErrorHandler(errors, warnings);
            validator.setErrorHandler(handler);

            // Configure a secure SAX parser to avoid XXE attacks
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);

            XMLReader reader = factory.newSAXParser().getXMLReader();
            Source source = new SAXSource(reader, new InputSource(new BufferedInputStream(inputStream)));
            validator.validate(source);

            resultBuilder.valid(!handler.hasErrors());
            resultBuilder.errors(errors);
            resultBuilder.warnings(warnings);
            resultBuilder.validatedAgainst("XSD " + schemaVersion.getVersion());
            resultBuilder.validationTimeMs(System.currentTimeMillis() - startTime);

            return resultBuilder.build();

        } catch (Exception e) {
            logger.error("Échec de la validation", e);
            resultBuilder.valid(false);
            resultBuilder.validationTimeMs(System.currentTimeMillis() - startTime);
            ValidationError error = ValidationError.builder()
                    .message("Erreur de validation : " + e.getMessage())
                    .severity(ValidationError.ErrorSeverity.FATAL)
                    .build();
            errors.add(error);
            resultBuilder.errors(errors);
            resultBuilder.warnings(warnings);
            return resultBuilder.build();
        }
    }

    private Schema getSchemaForVersion(SchemaVersion version, MessageType type) throws SAXException, IOException {
        String key = version.getVersion() + "-" + type.name();
        Schema cached = schemaCache.get(key);
        if (cached != null) {
            return cached;
        }

        String resourcePath = "/xsd/" + version.getVersion() + "/uncefact/data/standard/" + getSchemaFileName(type, version);
        java.net.URL xsdUrl = com.cii.messaging.model.util.UneceSchemaLoader.class.getResource(resourcePath);
        if (xsdUrl == null) {
            logger.warn("Schéma XSD non disponible pour la version : {} type : {}", version.getVersion(), type);
            throw new SAXException("Schéma XSD introuvable pour la version : " + version.getVersion() + " type : " + type);
        }
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
            // Ignore
        }
        Schema schema = factory.newSchema(xsdUrl);
        Schema existing = schemaCache.putIfAbsent(key, schema);
        return existing != null ? existing : schema;
    }
    
    private String getSchemaFileName(MessageType type, SchemaVersion version) {
        switch (version) {
            case D23B:
                switch (type) {
                    case INVOICE:
                        return "CrossIndustryInvoice_26p1.xsd";
                    case DESADV:
                        return "CrossIndustryDespatchAdvice_25p1.xsd";
                    case ORDER:
                        return "CrossIndustryOrder_25p1.xsd";
                    case ORDERSP:
                        return "CrossIndustryOrderResponse_25p1.xsd";
                    default:
                        throw new IllegalArgumentException("Type de message non pris en charge : " + type);
                }
            case D16B:
                switch (type) {
                    case INVOICE:
                        return "CrossIndustryInvoice_13p1.xsd";
                    case DESADV:
                        return "CrossIndustryDespatchAdvice_12p1.xsd";
                    case ORDER:
                        return "CrossIndustryOrder_12p1.xsd";
                    case ORDERSP:
                        return "CrossIndustryOrderResponse_12p1.xsd";
                    default:
                        throw new IllegalArgumentException("Type de message non pris en charge : " + type);
                }
            default:
                throw new IllegalArgumentException("Version de schéma non prise en charge : " + version);
        }
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
            if ("CrossIndustryInvoice".equals(root)) {
                return MessageType.INVOICE;
            } else if ("CrossIndustryDespatchAdvice".equals(root)) {
                return MessageType.DESADV;
            } else if ("CrossIndustryOrder".equals(root)) {
                return MessageType.ORDER;
            } else if ("CrossIndustryOrderResponse".equals(root)) {
            return MessageType.ORDERSP;
        }
        throw new SAXException("Élément racine inconnu : " + root);
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
