package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.validator.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeValidatorTest {

    private static class RecordingValidator implements CIIValidator {
        private final ValidationResult result;
        private String lastInput;

        RecordingValidator(ValidationResult result) {
            this.result = result;
        }

        @Override
        public ValidationResult validate(File xmlFile) {
            try (InputStream is = new FileInputStream(xmlFile)) {
                return validate(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ValidationResult validate(InputStream inputStream) {
            try {
                lastInput = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return result;
        }

        @Override
        public ValidationResult validate(String xmlContent) {
            lastInput = xmlContent;
            return result;
        }

        @Override
        public ValidationResult validate(CIIMessage message) {
            lastInput = "message";
            return result;
        }

        @Override
        public void setSchemaVersion(SchemaVersion version) {
        }
    }

    @Test
    void allOverloadsAggregateChildResultsConsistently() throws Exception {
        ValidationWarning warning = ValidationWarning.builder().message("warn").build();
        ValidationError error = ValidationError.builder()
                .message("err")
                .severity(ValidationError.ErrorSeverity.ERROR)
                .build();

        ValidationResult res1 = ValidationResult.builder()
                .valid(true)
                .warnings(List.of(warning))
                .validatedAgainst("V1")
                .build();

        ValidationResult res2 = ValidationResult.builder()
                .valid(false)
                .errors(List.of(error))
                .validatedAgainst("V2")
                .build();

        RecordingValidator v1 = new RecordingValidator(res1);
        RecordingValidator v2 = new RecordingValidator(res2);

        CompositeValidator composite = new CompositeValidator();

        // Remove default validators
        java.lang.reflect.Field field = CompositeValidator.class.getDeclaredField("validators");
        field.setAccessible(true);
        List<CIIValidator> list = (List<CIIValidator>) field.get(composite);
        list.clear();

        composite.addValidator(v1);
        composite.addValidator(v2);

        String xml = "<test/>";

        ValidationResult expected = ValidationResult.builder()
                .valid(false)
                .errors(List.of(error))
                .warnings(List.of(warning))
                .validatedAgainst("V1, V2")
                .build();

        // File
        File temp = Files.createTempFile("cii", ".xml").toFile();
        Files.writeString(temp.toPath(), xml, StandardCharsets.UTF_8);
        ValidationResult fileResult = composite.validate(temp);
        assertEquals(expected, fileResult);
        assertEquals(xml, v1.lastInput);
        assertEquals(xml, v2.lastInput);

        // InputStream
        ValidationResult streamResult = composite.validate(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected, streamResult);
        assertEquals(xml, v1.lastInput);
        assertEquals(xml, v2.lastInput);

        // String
        ValidationResult stringResult = composite.validate(xml);
        assertEquals(expected, stringResult);
        assertEquals(xml, v1.lastInput);
        assertEquals(xml, v2.lastInput);

        // CIIMessage
        CIIMessage msg = CIIMessage.builder()
                .messageId("1")
                .messageType(MessageType.INVOICE)
                .creationDateTime(LocalDateTime.now())
                .senderPartyId("S")
                .receiverPartyId("R")
                .build();

        ValidationResult msgResult = composite.validate(msg);
        assertEquals(expected, msgResult);
        assertEquals(v1.lastInput, v2.lastInput);
        assertNotNull(v1.lastInput);
    }
}

