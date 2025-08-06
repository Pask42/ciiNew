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
    description = "Convert between XML and JSON formats"
)
public class ConvertCommand extends AbstractCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ConvertCommand.class);
    
    @Parameters(index = "0", description = "Input file")
    private File inputFile;
    
    @Option(names = {"-o", "--output"}, description = "Output file", required = true)
    private File outputFile;

    @Option(names = {"-t", "--to"}, description = "Target format: XML or JSON", required = true)
    private TargetFormat targetFormat;
    
    @Option(names = {"--type"}, description = "Message type (required for JSON to XML)")
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
            logger.error("Input file not found: {}", inputFile);
            return 1;
        }
      
       if (!inputFile.canRead()) {
            logger.error("Cannot read input file: " + inputFile);
            return 1;
        }


        logger.info("Converting {} to {}...", inputFile.getName(), targetFormat);
        
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
                System.err.println("Input is neither valid JSON nor XML");
                if (jsonError != null) {
                    System.err.println("JSON parse error: " + jsonError.getMessage());
                }
                if (xmlError != null) {
                    System.err.println("XML parse error: " + xmlError.getMessage());
                }
                return 1;
            }

            java.io.File parent = outputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            
            if (targetFormat == TargetFormat.JSON) {
                if (isJson) {
                    logger.error("Input is already JSON");
                    return 1;
                }
                // XML to JSON
                CIIMessage message = service.readMessage(inputFile);
                String json = service.convertToJson(message);
                Files.writeString(outputFile.toPath(), json, StandardCharsets.UTF_8);

            } else if (targetFormat == TargetFormat.XML) {
                if (isXml) {
                    logger.error("Input is already XML");
                    return 1;
                }
                // JSON to XML
                if (messageType == null) {
                    logger.error("Message type required for JSON to XML conversion (use --type)");
                    return 1;
                }
                CIIMessage message = service.convertFromJson(inputContent, messageType);
                service.writeMessage(message, outputFile);

            } else {
                logger.error("Invalid target format: {}", targetFormat);
                return 1;
            }

            logger.info("Conversion successful! Output saved to: {}", outputFile.getAbsolutePath());

            return 0;

        } catch (Exception e) {
            logger.error("Conversion failed: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Cause: {}", e.getCause().getMessage());
            }
            return 1;
        }
    }
}
