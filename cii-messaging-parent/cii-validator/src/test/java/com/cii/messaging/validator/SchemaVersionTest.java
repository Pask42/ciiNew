package com.cii.messaging.validator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchemaVersionTest {

    private String originalSystemProperty;

    @BeforeEach
    void captureProperty() {
        originalSystemProperty = System.getProperty("unece.version");
    }

    @AfterEach
    void restoreProperty() {
        if (originalSystemProperty != null) {
            System.setProperty("unece.version", originalSystemProperty);
        } else {
            System.clearProperty("unece.version");
        }
    }

    @Test
    void systemPropertyOverridesDefaultVersion() {
        System.setProperty("unece.version", "D24A");
        assertEquals(SchemaVersion.D24A, SchemaVersion.getDefault());
    }

    @Test
    void fallsBackToBundledVersionWhenNoOverride() {
        System.clearProperty("unece.version");
        assertEquals(SchemaVersion.D23B, SchemaVersion.getDefault());
}
}
