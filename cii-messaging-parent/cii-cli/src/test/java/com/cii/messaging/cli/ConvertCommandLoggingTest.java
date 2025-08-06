package com.cii.messaging.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class ConvertCommandLoggingTest {

    @AfterEach
    void resetLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    void logsErrorWhenInputFileMissing() {
        ConvertCommand cmd = new ConvertCommand();
        CommandLine cl = new CommandLine(cmd);

        Logger logger = (Logger) LoggerFactory.getLogger(ConvertCommand.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        int exitCode = cl.execute("missing.xml", "--output", "out.json", "--to", "JSON");
        assertEquals(1, exitCode);

        boolean errorLogged = appender.list.stream()
                .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains("Input file not found"));
        assertTrue(errorLogged);

        logger.detachAppender(appender);
    }
}
