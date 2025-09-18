package com.cii.messaging.validator;

import java.io.File;
import java.io.InputStream;

/**
 * Interface générique de validation pour les messages CII représentés en XML.
 */
public interface CIIValidator {
    ValidationResult validate(File xmlFile);
    ValidationResult validate(InputStream inputStream);
    ValidationResult validate(String xmlContent);
    void setSchemaVersion(SchemaVersion version);
}
