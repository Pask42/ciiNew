package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.validator.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeValidatorTest {

    private static class RecordingValidator implements CIIValidator {
        private final ValidationResult result;
        private String lastInput;

        RecordingValidator(ValidationResult result) {
            this.result = result;
        }

        private void pause() {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
            pause();
            return result;
        }

        @Override
        public ValidationResult validate(String xmlContent) {
            lastInput = xmlContent;
            pause();
            return result;
        }

        @Override
        public ValidationResult validate(CIIMessage message) {
            lastInput = "message";
            pause();
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
        assertEquals(expected.isValid(), fileResult.isValid());
        assertEquals(expected.getErrors(), fileResult.getErrors());
        assertEquals(expected.getWarnings(), fileResult.getWarnings());
        assertEquals(expected.getValidatedAgainst(), fileResult.getValidatedAgainst());
        assertTrue(fileResult.getValidationTimeMs() > 0);
        assertEquals(xml, v1.lastInput);
        assertEquals(xml, v2.lastInput);

        // InputStream
        ValidationResult streamResult = composite.validate(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected.isValid(), streamResult.isValid());
        assertEquals(expected.getErrors(), streamResult.getErrors());
        assertEquals(expected.getWarnings(), streamResult.getWarnings());
        assertEquals(expected.getValidatedAgainst(), streamResult.getValidatedAgainst());
        assertTrue(streamResult.getValidationTimeMs() > 0);
        assertEquals(xml, v1.lastInput);
        assertEquals(xml, v2.lastInput);

        // String
        ValidationResult stringResult = composite.validate(xml);
        assertEquals(expected.isValid(), stringResult.isValid());
        assertEquals(expected.getErrors(), stringResult.getErrors());
        assertEquals(expected.getWarnings(), stringResult.getWarnings());
        assertEquals(expected.getValidatedAgainst(), stringResult.getValidatedAgainst());
        assertTrue(stringResult.getValidationTimeMs() > 0);
        assertEquals(xml, v1.lastInput);
        assertEquals(xml, v2.lastInput);

        // CIIMessage
        CIIMessage msg = CIIMessage.builder()
                .messageId("1")
                .messageType(MessageType.INVOICE)
                  .creationDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .senderPartyId("S")
                .receiverPartyId("R")
                .build();

        ValidationResult msgResult = composite.validate(msg);
        assertEquals(expected.isValid(), msgResult.isValid());
        assertEquals(expected.getErrors(), msgResult.getErrors());
        assertEquals(expected.getWarnings(), msgResult.getWarnings());
        assertEquals(expected.getValidatedAgainst(), msgResult.getValidatedAgainst());
        assertTrue(msgResult.getValidationTimeMs() > 0);
        assertEquals(v1.lastInput, v2.lastInput);
        assertNotNull(v1.lastInput);
    }
}

