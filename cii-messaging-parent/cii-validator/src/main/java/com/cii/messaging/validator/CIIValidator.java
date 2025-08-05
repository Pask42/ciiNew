package com.cii.messaging.validator;

import com.cii.messaging.model.CIIMessage;
import java.io.File;
import java.io.InputStream;

public interface CIIValidator {
    ValidationResult validate(File xmlFile);
    ValidationResult validate(InputStream inputStream);
    ValidationResult validate(String xmlContent);
    ValidationResult validate(CIIMessage message);
    void setSchemaVersion(SchemaVersion version);
}
