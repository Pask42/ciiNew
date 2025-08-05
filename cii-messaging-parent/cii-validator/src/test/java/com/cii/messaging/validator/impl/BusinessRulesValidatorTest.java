package com.cii.messaging.validator.impl;

import com.cii.messaging.validator.ValidationError;
import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BusinessRulesValidatorTest {

    private String readSample() throws IOException {
        return Files.readString(Path.of("src", "test", "resources", "invoice-sample.xml"));
    }

    @Test
    void validateStringValidXml() throws Exception {
        String xml = readSample();
        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(xml);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateInputStreamValidXml() throws Exception {
        BusinessRulesValidator validator = new BusinessRulesValidator();
        try (InputStream is = Files.newInputStream(Path.of("src", "test", "resources", "invoice-sample.xml"))) {
            ValidationResult result = validator.validate(is);
            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }
    }

    @Test
    void validateStringInvalidXmlProducesFatalError() {
        String xml = "<invalid>";
        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(xml);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(ValidationError.ErrorSeverity.FATAL, result.getErrors().get(0).getSeverity());
    }

    @Test
    void validateInputStreamInvalidXmlProducesFatalError() {
        InputStream is = new ByteArrayInputStream("<invalid>".getBytes(StandardCharsets.UTF_8));
        BusinessRulesValidator validator = new BusinessRulesValidator();
        ValidationResult result = validator.validate(is);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(ValidationError.ErrorSeverity.FATAL, result.getErrors().get(0).getSeverity());
    }
}
