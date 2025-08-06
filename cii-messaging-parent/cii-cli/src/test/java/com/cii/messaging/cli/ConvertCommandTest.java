package com.cii.messaging.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.service.CIIMessagingService;
import com.cii.messaging.service.impl.CIIMessagingServiceImpl;

import picocli.CommandLine;

public class ConvertCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void failsWhenInputAlreadyJson() throws Exception {
        Path input = tempDir.resolve("input.json");
        Files.writeString(input, "{}");
        Path output = tempDir.resolve("out.json");

        Logger logger = (Logger) LoggerFactory.getLogger(ConvertCommand.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            int exitCode = new CommandLine(new ConvertCommand())
                    .execute(input.toString(), "-o", output.toString(), "-t", "JSON");
            assertEquals(1, exitCode);
            boolean logged = appender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("Input is already JSON"));
            assertTrue(logged);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void failsWhenInputAlreadyXml() throws Exception {
        Path input = tempDir.resolve("input.xml");
        Files.writeString(input, "<root/>");
        Path output = tempDir.resolve("out.xml");

        Logger logger = (Logger) LoggerFactory.getLogger(ConvertCommand.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            int exitCode = new CommandLine(new ConvertCommand())
                    .execute(input.toString(), "-o", output.toString(), "-t", "XML");
            assertEquals(1, exitCode);
            boolean logged = appender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("Input is already XML"));
            assertTrue(logged);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void failsWhenInputMalformed() throws Exception {
        Path input = tempDir.resolve("input.txt");
        Files.writeString(input, "not json or xml");
        Path output = tempDir.resolve("out.json");

        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        try {
            int exitCode = new CommandLine(new ConvertCommand())
                    .execute(input.toString(), "-o", output.toString(), "-t", "JSON");
            assertEquals(1, exitCode);
            assertTrue(err.toString().contains("neither valid JSON nor XML"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void convertsXmlToJson() throws Exception {
        Path sample = Path.of("..", "cii-samples", "src", "main", "resources", "samples", "invoice-sample.xml");
        Path input = tempDir.resolve("input.xml");
        Files.copy(sample, input);
        Path output = tempDir.resolve("out.json");

        int exitCode = new CommandLine(new ConvertCommand())
                .execute(input.toString(), "-o", output.toString(), "-t", "JSON");
        assertEquals(0, exitCode);
        String json = Files.readString(output);
        assertTrue(json.contains("\"messageType\":\"INVOICE\""));
    }

    @Test
    void convertsJsonToXml() throws Exception {
        Path sample = Path.of("..", "cii-samples", "src", "main", "resources", "samples", "invoice-sample.xml");
        CIIMessagingService service = new CIIMessagingServiceImpl();
        CIIMessage message = service.readMessage(sample.toFile());
        String jsonContent = service.convertToJson(message);
        Path input = tempDir.resolve("input.json");
        Files.writeString(input, jsonContent);
        Path output = tempDir.resolve("out.xml");

        int exitCode = new CommandLine(new ConvertCommand())
                .execute(input.toString(), "-o", output.toString(), "-t", "XML", "--type", "INVOICE");
        assertEquals(0, exitCode);
        String xml = Files.readString(output);
        assertTrue(xml.contains("CrossIndustryInvoice"));
    }
}
