package com.cii.messaging.validator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationWarning {
    private String message;
    private String location;
    private String rule;
}
