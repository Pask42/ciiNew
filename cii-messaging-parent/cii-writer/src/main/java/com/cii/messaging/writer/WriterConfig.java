package com.cii.messaging.writer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WriterConfig {
    @Builder.Default
    private boolean formatOutput = true;
    
    @Builder.Default
    private String encoding = "UTF-8";
    
    @Builder.Default
    private boolean includeNamespaces = true;
    
    @Builder.Default
    private String indentation = "  ";
}
