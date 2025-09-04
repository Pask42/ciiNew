package com.cii.messaging.cli;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.service.CIIMessagingService;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import picocli.CommandLine.*;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(
    name = "convert",
    description = "Convertir entre formats XML et JSON"
)
public class ConvertCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ConvertCommand.class);
    
    @Parameters(index = "0", description = "Fichier d'entrée")
    private File inputFile;
    
    @Option(names = {"-o", "--output"}, description = "Fichier de sortie", required = true)
    private File outputFile;

    @Option(names = {"-t", "--to"}, description = "Format cible : XML ou JSON", required = true)
    private TargetFormat targetFormat;
    
    @Option(names = {"--type"}, description = "Type de message (requis pour JSON vers XML)")
    private MessageType messageType;
    
    private final CIIMessagingService service = new CIIMessagingServiceImpl();

    /**
     * Supported output formats for conversion.
     */
    enum TargetFormat {
        XML, JSON
    }
    
    /**
     * Executes the conversion.
     *
     * @return 0 on success, 1 on error
     * @throws Exception if unexpected errors occur
     */
    @Override
    public Integer call() throws Exception {
        configureLogging();

        if (!inputFile.exists()) {
            logger.error("Fichier d'entrée introuvable : {}", inputFile);
            return 1;
        }

       if (!inputFile.canRead()) {
            logger.error("Impossible de lire le fichier d'entrée : " + inputFile);
            return 1;
        }


        logger.info("Conversion de {} vers {}...", inputFile.getName(), targetFormat);
        
        try {
            String inputContent = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
            boolean isJson = false;
            boolean isXml = false;
            Exception jsonError = null;
            Exception xmlError = null;

            try {
                new ObjectMapper().readTree(inputContent);
                isJson = true;
            } catch (JsonProcessingException e) {
                jsonError = e;
            }

            if (!isJson) {
                try {
                    DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(new InputSource(new StringReader(inputContent)));
                    isXml = true;
                } catch (Exception e) {
                    xmlError = e;
                }
            }

            if (!isJson && !isXml) {
                System.err.println("L'entrée n'est ni un JSON valide ni un XML");
                if (jsonError != null) {
                    System.err.println("Erreur d'analyse JSON : " + jsonError.getMessage());
                }
                if (xmlError != null) {
                    System.err.println("Erreur d'analyse XML : " + xmlError.getMessage());
                }
                return 1;
            }

            java.io.File parent = outputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            
            if (targetFormat == TargetFormat.JSON) {
                if (isJson) {
                    logger.error("L'entrée est déjà au format JSON");
                    return 1;
                }
                // XML to JSON
                CIIMessage message = service.readMessage(inputFile);
                String json = service.convertToJson(message);
                Files.writeString(outputFile.toPath(), json, StandardCharsets.UTF_8);

            } else if (targetFormat == TargetFormat.XML) {
                if (isXml) {
                    logger.error("L'entrée est déjà au format XML");
                    return 1;
                }
                // JSON to XML
                if (messageType == null) {
                    logger.error("Type de message requis pour la conversion JSON vers XML (utiliser --type)");
                    return 1;
                }
                CIIMessage message = service.convertFromJson(inputContent, messageType);
                service.writeMessage(message, outputFile);

            } else {
                logger.error("Format cible invalide : {}", targetFormat);
                return 1;
            }

            logger.info("Conversion réussie ! Résultat enregistré dans : {}", outputFile.getAbsolutePath());

            return 0;

        } catch (Exception e) {
            logger.error("Échec de la conversion : {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Cause : {}", e.getCause().getMessage());
            }
            return 1;
        }
    }
}
