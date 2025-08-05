package com.cii.messaging.cli;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.service.CIIMessagingService;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.*;
import java.io.File;
import java.util.concurrent.Callable;

@Command(
    name = "parse",
    description = "Parse CII messages and extract data"
)
public class ParseCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "Input XML file to parse")
    private File inputFile;
    
    @Option(names = {"-o", "--output"}, description = "Output file (optional)")
    private File outputFile;
    
    @Option(names = {"--format"}, description = "Output format: JSON or SUMMARY", defaultValue = "SUMMARY")
    private String format;
    
    private final CIIMessagingService service = new CIIMessagingServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public Integer call() throws Exception {
        if (!inputFile.exists()) {
            System.err.println("Input file not found: " + inputFile);
            return 1;
        }
        
        System.out.println("Parsing " + inputFile.getName() + "...");
        
        try {
            CIIMessage message = service.readMessage(inputFile);
            
            String output;
            if ("JSON".equalsIgnoreCase(format)) {
                output = service.convertToJson(message);
            } else {
                output = generateSummary(message);
            }
            
            if (outputFile != null) {
                java.nio.file.Files.writeString(outputFile.toPath(), output,
                        java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("Output saved to: " + outputFile.getAbsolutePath());
            } else {
                System.out.println("\n" + output);
            }
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Failed to parse file: " + e.getMessage());
            return 1;
        }
    }
    
    private String generateSummary(CIIMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CII Message Summary ===\n");
        sb.append("Message Type: ").append(message.getMessageType()).append("\n");
        sb.append("Message ID: ").append(message.getMessageId()).append("\n");
        sb.append("Created: ").append(message.getCreationDateTime()).append("\n");
        sb.append("Sender: ").append(message.getSenderPartyId()).append("\n");
        sb.append("Receiver: ").append(message.getReceiverPartyId()).append("\n");
        
        if (message.getHeader() != null) {
            sb.append("\n--- Document Header ---\n");
            sb.append("Document Number: ").append(message.getHeader().getDocumentNumber()).append("\n");
            sb.append("Document Date: ").append(message.getHeader().getDocumentDate()).append("\n");
            sb.append("Currency: ").append(message.getHeader().getCurrency()).append("\n");
        }
        
        if (message.getLineItems() != null && !message.getLineItems().isEmpty()) {
            sb.append("\n--- Line Items (").append(message.getLineItems().size()).append(") ---\n");
            message.getLineItems().forEach(item -> {
                sb.append("  Line ").append(item.getLineNumber()).append(": ")
                  .append(item.getDescription()).append(" - ")
                  .append(item.getQuantity()).append(" ").append(item.getUnitCode())
                  .append(" @ ").append(item.getUnitPrice()).append(" = ")
                  .append(item.getLineAmount()).append("\n");
            });
        }
        
        if (message.getTotals() != null) {
            sb.append("\n--- Totals ---\n");
            sb.append("Line Total: ").append(message.getTotals().getLineTotalAmount()).append("\n");
            sb.append("Tax Total: ").append(message.getTotals().getTaxTotalAmount()).append("\n");
            sb.append("Grand Total: ").append(message.getTotals().getGrandTotalAmount()).append("\n");
        }
        
        return sb.toString();
    }
}
