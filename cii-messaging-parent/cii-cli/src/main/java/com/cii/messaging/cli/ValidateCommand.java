package com.cii.messaging.cli;

import com.cii.messaging.validator.SchemaVersion;
import com.cii.messaging.validator.ValidationResult;
import com.cii.messaging.validator.ValidationWarning;
import com.cii.messaging.validator.impl.CompositeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "validate", description = "Valider un fichier XML CII contre les schémas UNECE")
public class ValidateCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);

    @Parameters(index = "0", paramLabel = "INPUT", description = "Fichier XML à valider")
    private Path inputFile;

    @Option(names = "--schema-version", paramLabel = "VERSION",
            description = "Version de schéma UNECE à utiliser (ex: D23B, D24A)")
    private String schemaVersion;

    @Option(names = "--fail-on-warning", description = "Considère les avertissements comme des erreurs")
    private boolean failOnWarning;

    @Override
    public Integer call() throws Exception {
        configureLogging();

        Path resolvedInput = inputFile.toAbsolutePath().normalize();
        if (!Files.exists(resolvedInput) || !Files.isRegularFile(resolvedInput)) {
            logger.error("Fichier d'entrée introuvable : {}", resolvedInput);
            return 1;
        }
        if (!Files.isReadable(resolvedInput)) {
            logger.error("Fichier d'entrée illisible : {}", resolvedInput);
            return 1;
        }

        SchemaVersion version;
        try {
            version = schemaVersion != null && !schemaVersion.isBlank()
                    ? SchemaVersion.fromString(schemaVersion)
                    : SchemaVersion.getDefault();
        } catch (IllegalArgumentException ex) {
            logger.error("Version de schéma inconnue : {}", schemaVersion);
            logger.debug("Version invalide", ex);
            return 1;
        }

        CompositeValidator validator = new CompositeValidator();
        validator.setSchemaVersion(version);
        ValidationResult result = validator.validate(resolvedInput.toFile());

        logValidationSummary(result, version);
        return determineExitCode(result);
    }

    private void logValidationSummary(ValidationResult result, SchemaVersion version) {
        int errorCount = result.getErrors() != null ? result.getErrors().size() : 0;
        if (result.isValid()) {
            logger.info("Validation réussie avec la version {}", version.getVersion());
        } else {
            logger.error("Validation échouée ({} erreurs)", errorCount);
        }

        if (result.getValidatedAgainst() != null) {
            logger.info("Schémas utilisés : {}", result.getValidatedAgainst());
        }
        if (result.getValidationTimeMs() > 0) {
            logger.info("Durée de validation : {} ms", result.getValidationTimeMs());
        }

        if (errorCount > 0) {
            result.getErrors().forEach(err -> logger.error("{}", err.getMessage()));
        }
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            result.getWarnings().stream()
                    .map(ValidationWarning::getMessage)
                    .forEach(warning -> logger.warn("{}", warning));
        }
    }

    int determineExitCode(ValidationResult result) {
        if (!result.isValid()) {
            return 1;
        }
        if (failOnWarning && result.hasWarnings()) {
            return 1;
        }
        return 0;
    }
}
