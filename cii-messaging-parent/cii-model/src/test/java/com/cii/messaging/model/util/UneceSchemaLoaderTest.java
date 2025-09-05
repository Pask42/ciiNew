package com.cii.messaging.model.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UneceSchemaLoaderTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty(UneceSchemaLoader.PROPERTY);
    }

    @Test
    void defaultVersionIsD23B() {
        assertDoesNotThrow(() -> UneceSchemaLoader.loadSchema("CrossIndustryInvoice.xsd"));
    }

    @Test
    void loadD16BWhenSpecified() {
        System.setProperty(UneceSchemaLoader.PROPERTY, "D16B");
        assertDoesNotThrow(() -> UneceSchemaLoader.loadSchema("CrossIndustryInvoice.xsd"));
    }

    @Test
    void unknownVersionThrows() {
        System.setProperty(UneceSchemaLoader.PROPERTY, "D99Z");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                UneceSchemaLoader.loadSchema("QualifiedDataType_34p0.xsd"));
        assertTrue(ex.getMessage().contains("D99Z"));
    }

    @Test
    void loadingWrongVersionThrows() {
        System.setProperty(UneceSchemaLoader.PROPERTY, "D16B");
        assertThrows(IllegalArgumentException.class, () ->
                UneceSchemaLoader.loadSchema("CrossIndustryInvoice_26p1.xsd"));
    }
}

