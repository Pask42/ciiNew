package com.cii.messaging.model.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class UneceSchemaLoaderTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty(UneceSchemaLoader.PROPERTY);
    }

    private void validate(String xml) throws Exception {
        Validator validator = UneceSchemaLoader.loadSchema("CrossIndustryInvoice.xsd").newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
    }

    @Test
    void defaultVersionIsD23B() throws Exception {
        String valid = "<Invoice><Seller>ACME</Seller></Invoice>";
        validate(valid); // should not throw
    }

    @Test
    void loadD16BWhenSpecified() throws Exception {
        System.setProperty(UneceSchemaLoader.PROPERTY, "D16B");
        String valid = "<Invoice><Buyer>ACME</Buyer></Invoice>";
        validate(valid);
    }

    @Test
    void unknownVersionThrows() {
        System.setProperty(UneceSchemaLoader.PROPERTY, "D99Z");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                UneceSchemaLoader.loadSchema("CrossIndustryInvoice.xsd"));
        assertTrue(ex.getMessage().contains("D99Z"));
    }

    @Test
    void validationFailsForWrongVersion() throws Exception {
        System.setProperty(UneceSchemaLoader.PROPERTY, "D16B");
        String invalid = "<Invoice><Seller>ACME</Seller></Invoice>";
        assertThrows(SAXException.class, () -> validate(invalid));
    }
}
