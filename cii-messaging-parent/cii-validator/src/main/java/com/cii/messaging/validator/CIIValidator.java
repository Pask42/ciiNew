package com.cii.messaging.validator;

import java.io.File;
import java.io.InputStream;

/**
 * Generic validator interface for CII messages represented as XML.
 */
public interface CIIValidator {
    ValidationResult validate(File xmlFile);
    ValidationResult validate(InputStream inputStream);
    ValidationResult validate(String xmlContent);
    void setSchemaVersion(SchemaVersion version);
}
