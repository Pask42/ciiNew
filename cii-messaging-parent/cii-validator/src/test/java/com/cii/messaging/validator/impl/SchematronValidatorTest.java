package com.cii.messaging.validator.impl;

import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SchematronValidatorTest {

    private final SchematronValidator validator = new SchematronValidator();

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
                <rsm:CrossIndustryInvoice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:16B\"
                    xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:16B\">
                    <rsm:ExchangedDocument/>
                    <rsm:SupplyChainTradeTransaction/>
                </rsm:CrossIndustryInvoice>
                """;
        ValidationResult result = validator.validate(xml);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
    }
}

