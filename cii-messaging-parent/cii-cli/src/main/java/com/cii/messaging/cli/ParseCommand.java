package com.cii.messaging.cli;

import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.reader.CIIReaderFactory;
import com.cii.messaging.reader.CIIReaderException;
import picocli.CommandLine.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "parse", description = "Analyser un message CII et afficher son contenu")
public class ParseCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ParseCommand.class);

    @Parameters(index = "0", description = "Fichier XML d'entrée à analyser")
    private File inputFile;

    @Option(names = {"-o", "--output"}, description = "Fichier de sortie (optionnel)")
    private File outputFile;

    @Option(names = {"--format"}, description = "Format de sortie : JSON ou SUMMARY", defaultValue = "SUMMARY")
    private OutputFormat format = OutputFormat.SUMMARY;

    @Override
    public Integer call() throws Exception {
        configureLogging();

        if (!inputFile.exists() || !inputFile.canRead()) {
            logger.error("Fichier d'entrée introuvable ou illisible : {}", inputFile);
            return 1;
        }

        try {
            CIIReader<?> reader = CIIReaderFactory.createReader(inputFile.toPath());
            Object message = reader.read(inputFile);

            String output;
            if (format == OutputFormat.JSON) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();
                output = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
            } else {
                output = "Type de message : " + message.getClass().getSimpleName();
            }

            if (outputFile != null) {
                Files.writeString(outputFile.toPath(), output, StandardCharsets.UTF_8);
                logger.info("Sortie enregistrée dans : {}", outputFile.getAbsolutePath());
            } else {
                logger.info("\n{}", output);
            }

            return 0;
        } catch (CIIReaderException e) {
            logger.error("Impossible d'analyser le fichier : {}", e.getMessage());
            return 1;
        }
    }
}
