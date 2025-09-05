package com.cii.messaging.cli;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.service.CIIMessagingService;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;
import picocli.CommandLine.*;

import java.io.File;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(
    name = "parse",
    description = "Analyser les messages CII et extraire les données"
)
public class ParseCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ParseCommand.class);
    
    @Parameters(index = "0", description = "Fichier XML d'entrée à analyser")
    private File inputFile;
    
    @Option(names = {"-o", "--output"}, description = "Fichier de sortie (optionnel)")
    private File outputFile;

    @Option(names = {"--format"}, description = "Format de sortie : JSON ou SUMMARY", defaultValue = "SUMMARY")
    private OutputFormat format = OutputFormat.SUMMARY;
    
    private final CIIMessagingService service = new CIIMessagingServiceImpl();
    
    @Override
    public Integer call() throws Exception {
        configureLogging();

        if (!inputFile.exists()) {
            logger.error("Fichier d'entrée introuvable : {}", inputFile);
            return 1;
        }
        if (!inputFile.canRead()) {
              logger.error("Fichier d'entrée illisible : " + inputFile);
              return 1;
          }
        logger.info("Analyse de {}...", inputFile.getName());
        
        try {
            CIIMessage message = service.readMessage(inputFile);
            
            String output;
            if (format == OutputFormat.JSON) {
                output = service.convertToJson(message);
            } else {
                output = generateSummary(message);
            }
            
            if (outputFile != null) {
                java.io.File parent = outputFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                java.nio.file.Files.writeString(outputFile.toPath(), output,
                        java.nio.charset.StandardCharsets.UTF_8);
                logger.info("Sortie enregistrée dans : {}", outputFile.getAbsolutePath());

            } else {
                logger.info("\n{}", output);
            }
            
            return 0;
            
        } catch (Exception e) {
            logger.error("Impossible d'analyser le fichier : {}", e.getMessage());
            return 1;
        }
    }
    
    private String generateSummary(CIIMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Résumé du message CII ===\n");
        sb.append("Type de message : ").append(message.getMessageType()).append("\n");
        sb.append("ID du message : ").append(message.getMessageId()).append("\n");
        sb.append("Créé le : ").append(message.getCreationDateTime()).append("\n");
        sb.append("Expéditeur : ").append(message.getSenderPartyId()).append("\n");
        sb.append("Destinataire : ").append(message.getReceiverPartyId()).append("\n");
        
        if (message.getHeader() != null) {
            sb.append("\n--- En-tête du document ---\n");
            sb.append("Numéro de document : ").append(message.getHeader().getDocumentNumber()).append("\n");
            sb.append("Date du document : ").append(message.getHeader().getDocumentDate()).append("\n");
            sb.append("Devise : ").append(message.getHeader().getCurrency()).append("\n");
        }
        
        if (message.getLineItems() != null && !message.getLineItems().isEmpty()) {
            sb.append("\n--- Lignes (").append(message.getLineItems().size()).append(") ---\n");
            message.getLineItems().forEach(item -> {
                sb.append("  Ligne ").append(item.getLineNumber()).append(" : ")
                  .append(item.getDescription()).append(" - ")
                  .append(item.getQuantity()).append(" ").append(item.getUnitCode())
                  .append(" @ ").append(item.getUnitPrice()).append(" = ")
                  .append(item.getLineAmount()).append("\n");
            });
        }
        
        if (message.getTotals() != null) {
            sb.append("\n--- Totaux ---\n");
            sb.append("Total des lignes : ").append(message.getTotals().getLineTotalAmount()).append("\n");
            sb.append("Total des taxes : ").append(message.getTotals().getTaxTotalAmount()).append("\n");
            sb.append("Total général : ").append(message.getTotals().getGrandTotalAmount()).append("\n");
        }
        
        return sb.toString();
    }
}
