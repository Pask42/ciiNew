package com.cii.messaging.cli;

import com.cii.messaging.validator.ValidationResult;
import com.cii.messaging.validator.impl.CompositeValidator;
import picocli.CommandLine.*;

import java.io.File;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "validate", description = "Valider un fichier XML CII contre les schémas UNECE")
public class ValidateCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);

    @Parameters(index = "0", description = "Fichier XML à valider")
    private File inputFile;

    @Override
    public Integer call() throws Exception {
        configureLogging();

        if (!inputFile.exists() || !inputFile.canRead()) {
            logger.error("Fichier d'entrée introuvable ou illisible : {}", inputFile);
            return 1;
        }

        CompositeValidator validator = new CompositeValidator();
        ValidationResult result = validator.validate(inputFile);

        if (result.isValid()) {
            logger.info("Fichier valide.");
            return 0;
        } else {
            result.getErrors().forEach(err -> logger.error(err.getMessage()));
            result.getWarnings().forEach(warn -> logger.warn(warn.getMessage()));
            return 1;
        }
    }
}
