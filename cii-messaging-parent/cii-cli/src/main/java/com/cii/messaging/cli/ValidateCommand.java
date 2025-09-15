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
    description = "Valider les messages CII selon les XSD et les règles métier"
)
public class ValidateCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);
    
    @Parameters(index = "0", description = "Fichier(s) XML à valider", arity = "1..*")
    private File[] inputFiles;
    
    @Option(names = {"--schema"}, description = "Version de schéma : D23B, D24A")
    private SchemaVersion schemaVersion = SchemaVersion.getDefault();
    
    @Option(names = {"-v", "--verbose"}, description = "Afficher les résultats détaillés de validation")
    private boolean verbose;
    
    private final CIIMessagingService service = new CIIMessagingServiceImpl();

    @Spec
    private CommandSpec spec;
    
    @Override
    public Integer call() throws Exception {
        configureLogging();

        int totalFiles = inputFiles.length;
        int validFiles = 0;
      
       logger.info("Validation de " + totalFiles + " fichier(s) avec " + schemaVersion.getVersion() + "...\n");

        service.setSchemaVersion(schemaVersion);

        for (File file : inputFiles) {
            if (!file.exists()) {
                logger.error("Fichier introuvable : {}", file);
                continue;
            }
            if (!file.isFile()) {
                spec.commandLine().getErr().println("N'est pas un fichier : " + file);
                continue;
            }
            if (!file.canRead()) {
                spec.commandLine().getErr().println("Impossible de lire le fichier : " + file);
                continue;
            }

            logger.info("Validation : {}", file.getName());

            try {
                ValidationResult result = service.validateMessage(file);
                
                if (result.isValid()) {
                    logger.info("✓ VALIDE");
                    validFiles++;
                } else {
                    logger.warn("✗ INVALIDE");
                }
                
                if (verbose || !result.isValid()) {
                    printValidationDetails(result);
                }

                logger.info("");
                
            } catch (Exception e) {
                logger.error("✗ ERREUR : {}", e.getMessage());
                logger.info("");
            }
        }

        // Summary
        logger.info("=== Résumé de la validation ===");
        logger.info("Nombre total de fichiers : {}", totalFiles);
        logger.info("Fichiers valides : {}", validFiles);
        logger.info("Fichiers invalides : {}", (totalFiles - validFiles));
        
        return validFiles == totalFiles ? 0 : 1;
    }
    
    private void printValidationDetails(ValidationResult result) {
        if (result.hasErrors()) {
            logger.error("  Erreurs ({})", result.getErrors().size());
            result.getErrors().forEach(error -> {
                logger.error("    - {}", error.getMessage());
                if (error.getLocation() != null) {
                    logger.error("      Emplacement : {}", error.getLocation());
                }
            });
        }

        if (result.hasWarnings()) {
            logger.warn("  Avertissements ({})", result.getWarnings().size());
            result.getWarnings().forEach(warning -> {
                logger.warn("    - {}", warning.getMessage());
            });
        }

        logger.info("  Validé selon : {}", result.getValidatedAgainst());
        logger.info("  Temps de validation : {} ms", result.getValidationTimeMs());
    }
}
