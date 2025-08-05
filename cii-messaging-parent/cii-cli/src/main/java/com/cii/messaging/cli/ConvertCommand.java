package com.cii.messaging.cli;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.service.CIIMessagingService;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;
import picocli.CommandLine.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@Command(
    name = "convert",
    description = "Convert between XML and JSON formats"
)
public class ConvertCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Input file")
    private File inputFile;
    
    @Option(names = {"-o", "--output"}, description = "Output file", required = true)
    private File outputFile;
    
    @Option(names = {"-t", "--to"}, description = "Target format: XML or JSON", required = true)
    private String targetFormat;
    
    @Option(names = {"--type"}, description = "Message type (required for JSON to XML)")
    private MessageType messageType;
    
    private final CIIMessagingService service = new CIIMessagingServiceImpl();
    
    @Override
    public Integer call() throws Exception {
        if (!inputFile.exists()) {
            System.err.println("Input file not found: " + inputFile);
            return 1;
        }
        
        System.out.println("Converting " + inputFile.getName() + " to " + targetFormat + "...");
        
        try {
            String inputContent = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);
            boolean isInputJson = inputContent.trim().startsWith("{");
            
            if ("JSON".equalsIgnoreCase(targetFormat)) {
                if (isInputJson) {
                    System.err.println("Input is already JSON");
                    return 1;
                }
                // XML to JSON
                CIIMessage message = service.readMessage(inputFile);
                String json = service.convertToJson(message);
                Files.writeString(outputFile.toPath(), json, StandardCharsets.UTF_8);
                
            } else if ("XML".equalsIgnoreCase(targetFormat)) {
                if (!isInputJson) {
                    System.err.println("Input is already XML");
                    return 1;
                }
                // JSON to XML
                if (messageType == null) {
                    System.err.println("Message type required for JSON to XML conversion (use --type)");
                    return 1;
                }
                CIIMessage message = service.convertFromJson(inputContent, messageType);
                service.writeMessage(message, outputFile);
                
            } else {
                System.err.println("Invalid target format: " + targetFormat);
                return 1;
            }
            
            System.out.println("Conversion successful! Output saved to: " + outputFile.getAbsolutePath());
            return 0;
            
        } catch (Exception e) {
            System.err.println("Conversion failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
            return 1;
        }
    }
}
