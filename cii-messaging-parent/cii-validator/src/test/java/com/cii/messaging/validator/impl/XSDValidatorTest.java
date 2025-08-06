package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.reader.CIIReaderFactory;
import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class XSDValidatorTest {

    @Test
    void validateCIIMessageValid() throws Exception {
        String xml = Files.readString(Path.of("src", "test", "resources", "invoice-sample.xml"));
        CIIReader reader = CIIReaderFactory.createReader(MessageType.INVOICE);
        CIIMessage message = reader.read(xml);
        XSDValidator validator = new XSDValidator();
        ValidationResult result = validator.validate(message);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    }

    @Test
    void validateCIIMessageInvalid() {
        CIIMessage message = CIIMessage.builder()
                .messageId("INV-1")
                .messageType(MessageType.INVOICE)
                .creationDateTime(LocalDateTime.now())
                .build();
        XSDValidator validator = new XSDValidator();
        ValidationResult result = validator.validate(message);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }
}
