package com.cii.messaging.validator;

import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ValidationResult {
    private boolean valid;
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();
    @Builder.Default
    private List<ValidationWarning> warnings = new ArrayList<>();
    private String validatedAgainst;
    private long validationTimeMs;
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public void addError(ValidationError error) {
        errors.add(error);
        valid = false;
    }
    
    public void addWarning(ValidationWarning warning) {
        warnings.add(warning);
    }
}
