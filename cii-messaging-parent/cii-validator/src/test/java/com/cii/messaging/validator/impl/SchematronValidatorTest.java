package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.reader.CIIReaderFactory;
import com.cii.messaging.validator.ValidationResult;
import com.cii.messaging.model.util.UneceSchemaLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class SchematronValidatorTest {

    private final SchematronValidator validator = new SchematronValidator();

    @BeforeAll
    static void setup() {
        System.setProperty(UneceSchemaLoader.PROPERTY, "D23B");
    }

    @AfterAll
    static void cleanup() {
        System.clearProperty(UneceSchemaLoader.PROPERTY);
    }

    @Test
    void validDocumentHasNoErrors() throws Exception {
        String xml = Files.readString(Path.of("src", "test", "resources", "invoice-sample.xml"));
        ValidationResult result = validator.validate(xml);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void missingIdAndLineItemProducesWarningAndError() {
        String xml = """
                <rsm:CrossIndustryInvoice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:26\"
                    xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:34\">
                    <rsm:ExchangedDocument/>
                    <rsm:SupplyChainTradeTransaction/>
                </rsm:CrossIndustryInvoice>
                """;
        ValidationResult result = validator.validate(xml);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    void validCIIMessageHasNoErrors() throws Exception {
        String xml = Files.readString(Path.of("src", "test", "resources", "invoice-sample.xml"));
        CIIReader reader = CIIReaderFactory.createReader(MessageType.INVOICE);
        CIIMessage message = reader.read(xml);
        ValidationResult result = validator.validate(message);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void invalidCIIMessageProducesErrors() {
        CIIMessage message = CIIMessage.builder()
                .messageId("INV-1")
                .messageType(MessageType.INVOICE)
                  .creationDateTime(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        ValidationResult result = validator.validate(message);
        assertTrue(result.isValid());
    }
}

