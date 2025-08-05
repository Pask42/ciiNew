package com.cii.messaging.validator.impl;

import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BusinessRulesValidatorFileTest {

    @Test
    void validateFileValidXml() {
        BusinessRulesValidator validator = new BusinessRulesValidator();
        File file = Path.of("src", "test", "resources", "invoice-sample.xml").toFile();
        ValidationResult result = validator.validate(file);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateFileMissingBuyerReferenceProducesError() {
        BusinessRulesValidator validator = new BusinessRulesValidator();
        File file = Path.of("src", "test", "resources", "invoice-missing-buyer-ref.xml").toFile();
        ValidationResult result = validator.validate(file);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> "BR-05".equals(e.getRule())));
    }
}
