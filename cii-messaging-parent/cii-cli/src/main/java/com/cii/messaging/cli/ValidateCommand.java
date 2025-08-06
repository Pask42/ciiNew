package com.cii.messaging.cli;

import com.cii.messaging.service.CIIMessagingService;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;
import com.cii.messaging.validator.*;
import picocli.CommandLine.*;
import java.io.File;
import java.util.concurrent.Callable;

@Command(
    name = "validate",
    description = "Validate CII messages against XSD and business rules"
)
public class ValidateCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "XML file(s) to validate", arity = "1..*")
    private File[] inputFiles;
    
    @Option(names = {"--schema"}, description = "Schema version: D16B, D20B, D21B", defaultValue = "D16B")
    private SchemaVersion schemaVersion;
    
    @Option(names = {"-v", "--verbose"}, description = "Show detailed validation results")
    private boolean verbose;
    
    private final CIIMessagingService service = new CIIMessagingServiceImpl();
    
    @Override
    public Integer call() throws Exception {
        int totalFiles = inputFiles.length;
        int validFiles = 0;
        
        System.out.println("Validating " + totalFiles + " file(s) against " + schemaVersion.getVersion() + "...\n");

        service.setSchemaVersion(schemaVersion);

        for (File file : inputFiles) {
            if (!file.exists()) {
                System.err.println("File not found: " + file);
                continue;
            }
            if (!file.isFile()) {
                System.err.println("Not a file: " + file);
                continue;
            }
            if (!file.canRead()) {
                System.err.println("Cannot read file: " + file);
                continue;
            }

            System.out.println("Validating: " + file.getName());

            try {
                ValidationResult result = service.validateMessage(file);
                
                if (result.isValid()) {
                    System.out.println("✓ VALID");
                    validFiles++;
                } else {
                    System.out.println("✗ INVALID");
                }
                
                if (verbose || !result.isValid()) {
                    printValidationDetails(result);
                }
                
                System.out.println();
                
            } catch (Exception e) {
                System.err.println("✗ ERROR: " + e.getMessage());
                System.out.println();
            }
        }
        
        // Summary
        System.out.println("=== Validation Summary ===");
        System.out.println("Total files: " + totalFiles);
        System.out.println("Valid files: " + validFiles);
        System.out.println("Invalid files: " + (totalFiles - validFiles));
        
        return validFiles == totalFiles ? 0 : 1;
    }
    
    private void printValidationDetails(ValidationResult result) {
        if (result.hasErrors()) {
            System.out.println("  Errors (" + result.getErrors().size() + "):");
            result.getErrors().forEach(error -> {
                System.out.println("    - " + error.getMessage());
                if (error.getLocation() != null) {
                    System.out.println("      Location: " + error.getLocation());
                }
            });
        }
        
        if (result.hasWarnings()) {
            System.out.println("  Warnings (" + result.getWarnings().size() + "):");
            result.getWarnings().forEach(warning -> {
                System.out.println("    - " + warning.getMessage());
            });
        }
        
        System.out.println("  Validated against: " + result.getValidatedAgainst());
        System.out.println("  Validation time: " + result.getValidationTimeMs() + "ms");
    }
}
