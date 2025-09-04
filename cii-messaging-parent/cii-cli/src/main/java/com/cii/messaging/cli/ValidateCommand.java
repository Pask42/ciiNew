package com.cii.messaging.cli;

import com.cii.messaging.service.CIIMessagingService;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;
import com.cii.messaging.validator.*;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;
import java.io.File;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(
    name = "validate",
    description = "Validate CII messages against XSD and business rules"
)
public class ValidateCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);
    
    @Parameters(index = "0", description = "XML file(s) to validate", arity = "1..*")
    private File[] inputFiles;
    
    @Option(names = {"--schema"}, description = "Schema version: D16B, D23B")
    private SchemaVersion schemaVersion = SchemaVersion.getDefault();
    
    @Option(names = {"-v", "--verbose"}, description = "Show detailed validation results")
    private boolean verbose;
    
    private final CIIMessagingService service = new CIIMessagingServiceImpl();

    @Spec
    private CommandSpec spec;
    
    @Override
    public Integer call() throws Exception {
        configureLogging();

        int totalFiles = inputFiles.length;
        int validFiles = 0;
      
       logger.info("Validating " + totalFiles + " file(s) against " + schemaVersion.getVersion() + "...\n");

        service.setSchemaVersion(schemaVersion);

        for (File file : inputFiles) {
            if (!file.exists()) {
                logger.error("File not found: {}", file);
                continue;
            }
            if (!file.isFile()) {
                spec.commandLine().getErr().println("Not a file: " + file);
                continue;
            }
            if (!file.canRead()) {
                spec.commandLine().getErr().println("Cannot read file: " + file);
                continue;
            }

            logger.info("Validating: {}", file.getName());

            try {
                ValidationResult result = service.validateMessage(file);
                
                if (result.isValid()) {
                    logger.info("✓ VALID");
                    validFiles++;
                } else {
                    logger.warn("✗ INVALID");
                }
                
                if (verbose || !result.isValid()) {
                    printValidationDetails(result);
                }

                logger.info("");
                
            } catch (Exception e) {
                logger.error("✗ ERROR: {}", e.getMessage());
                logger.info("");
            }
        }

        // Summary
        logger.info("=== Validation Summary ===");
        logger.info("Total files: {}", totalFiles);
        logger.info("Valid files: {}", validFiles);
        logger.info("Invalid files: {}", (totalFiles - validFiles));
        
        return validFiles == totalFiles ? 0 : 1;
    }
    
    private void printValidationDetails(ValidationResult result) {
        if (result.hasErrors()) {
            logger.error("  Errors ({})", result.getErrors().size());
            result.getErrors().forEach(error -> {
                logger.error("    - {}", error.getMessage());
                if (error.getLocation() != null) {
                    logger.error("      Location: {}", error.getLocation());
                }
            });
        }

        if (result.hasWarnings()) {
            logger.warn("  Warnings ({})", result.getWarnings().size());
            result.getWarnings().forEach(warning -> {
                logger.warn("    - {}", warning.getMessage());
            });
        }

        logger.info("  Validated against: {}", result.getValidatedAgainst());
        logger.info("  Validation time: {}ms", result.getValidationTimeMs());
    }
}
