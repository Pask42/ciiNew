package com.cii.messaging.cli;

import com.cii.messaging.model.order.Order;
import com.cii.messaging.reader.OrderReader;
import com.cii.messaging.reader.CIIReaderException;
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.writer.generation.OrderResponseGenerationOptions;
import com.cii.messaging.writer.generation.OrderResponseGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Callable;

/**
 * Commande CLI générant automatiquement un ORDER_RESPONSE (ORDERSP) depuis un fichier ORDER.
 */
@Command(name = "respond", description = "Générer un ORDER_RESPONSE (ORDERSP) à partir d'un message ORDER")
public class RespondCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(RespondCommand.class);
    private static final DateTimeFormatter ISSUE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Parameters(index = "0", paramLabel = "INPUT", description = "Fichier ORDER XML d'entrée")
    private Path inputFile;

    @Option(names = {"-o", "--output"}, paramLabel = "FILE", description = "Fichier ORDER_RESPONSE de sortie")
    private Path outputFile;

    @Option(names = "--response-id", description = "Identifiant explicite du ORDER_RESPONSE généré")
    private String responseId;

    @Option(names = "--ack-code", description = "Code d'accusé de réception (ex: AP, RE)", defaultValue = "AP")
    private String acknowledgementCode = "AP";

    @Option(names = "--issue-date", description = "Date d'émission au format yyyyMMddHHmmss")
    private String issueDate;

    @Option(names = "--response-id-prefix", description = "Préfixe appliqué à l'identifiant si non fourni", defaultValue = "ORDRSP-")
    private String responseIdPrefix = "ORDRSP-";

    @Override
    public Integer call() {
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

        Path resolvedOutput = resolveOutputPath(resolvedInput);

        try {
            Order order = new OrderReader().read(resolvedInput.toFile());
            OrderResponseGenerationOptions options = buildOptions();
            String message = OrderResponseGenerator.genererOrderResponse(order, resolvedOutput.toString(), options);
            logger.info(message);
            return 0;
        } catch (CIIReaderException e) {
            logger.error("Impossible de lire le fichier ORDER : {}", e.getMessage());
            logger.debug("Erreur complète", e);
            return 1;
        } catch (DateTimeParseException e) {
            logger.error("Format de date invalide pour --issue-date (attendu yyyyMMddHHmmss) : {}", e.getParsedString());
            return 1;
        } catch (IOException | CIIWriterException e) {
            logger.error("Erreur lors de la génération du ORDER_RESPONSE : {}", e.getMessage());
            logger.debug("Erreur complète", e);
            return 1;
        }
    }

    private OrderResponseGenerationOptions buildOptions() {
        OrderResponseGenerationOptions.Builder builder = OrderResponseGenerationOptions.builder()
                .withResponseIdPrefix(responseIdPrefix)
                .withAcknowledgementCode(acknowledgementCode);

        if (responseId != null && !responseId.isBlank()) {
            builder.withResponseId(responseId.trim());
        }
        if (issueDate != null && !issueDate.isBlank()) {
            builder.withIssueDateTime(LocalDateTime.parse(issueDate.trim(), ISSUE_DATE_FORMAT));
        }
        return builder.build();
    }

    private Path resolveOutputPath(Path resolvedInput) {
        if (outputFile != null) {
            return outputFile.toAbsolutePath().normalize();
        }
        String fileName = resolvedInput.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String base = dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex) : ".xml";
        String targetName = base + "-ordersp" + extension;
        Path parent = resolvedInput.getParent();
        if (parent == null) {
            parent = Path.of(".");
        }
        return parent.resolve(targetName).toAbsolutePath().normalize();
    }
}
