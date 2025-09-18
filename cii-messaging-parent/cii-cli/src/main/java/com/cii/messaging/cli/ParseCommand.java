package com.cii.messaging.cli;

import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.reader.CIIReaderException;
import com.cii.messaging.reader.CIIReaderFactory;
import com.cii.messaging.reader.analysis.OrderAnalysisResult;
import com.cii.messaging.reader.analysis.OrderAnalyzer;
import com.cii.messaging.model.order.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "parse", description = "Analyser un message CII et afficher son contenu")
public class ParseCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ParseCommand.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Parameters(index = "0", paramLabel = "INPUT", description = "Fichier XML d'entrée à analyser")
    private Path inputFile;

    @Option(names = {"-o", "--output"}, paramLabel = "FILE", description = "Fichier de sortie (optionnel)")
    private Path outputFile;

    @Option(names = {"--format"}, description = "Format de sortie : JSON ou SUMMARY", defaultValue = "SUMMARY")
    private OutputFormat format = OutputFormat.SUMMARY;

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

        try {
            CIIReader<?> reader = CIIReaderFactory.createReader(resolvedInput);
            Object message = reader.read(resolvedInput.toFile());

            String output = renderOutput(resolvedInput, message);
            writeOutput(output);
            return 0;
        } catch (CIIReaderException e) {
            logger.error("Impossible d'analyser le fichier : {}", e.getMessage());
            logger.debug("Erreur complète", e);
            return 1;
        } catch (IOException e) {
            logger.error("Erreur d'entrée/sortie lors du traitement de {} : {}", resolvedInput, e.getMessage());
            logger.debug("Erreur complète", e);
            return 1;
        }
    }

    private String renderOutput(Path input, Object message) throws IOException, CIIReaderException {
        return switch (format) {
            case JSON -> OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(message);
            case SUMMARY -> buildSummary(input, message);
        };
    }

    private String buildSummary(Path input, Object message) throws IOException, CIIReaderException {
        if (message instanceof Order) {
            OrderAnalysisResult result = OrderAnalyzer.analyserOrder(input.toString());
            return result.toPrettyString();
        }
        return "Type de message : " + message.getClass().getSimpleName();
    }

    private void writeOutput(String output) throws IOException {
        if (outputFile != null) {
            Path target = outputFile.toAbsolutePath().normalize();
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, output, StandardCharsets.UTF_8);
            logger.info("Sortie enregistrée dans : {}", target);
        } else {
            logger.info("\n{}", output);
        }
    }
}
