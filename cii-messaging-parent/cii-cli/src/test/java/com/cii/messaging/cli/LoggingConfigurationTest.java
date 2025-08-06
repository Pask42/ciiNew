package com.cii.messaging.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LoggingConfigurationTest {

    static class DummyCommand extends AbstractCommand implements java.util.concurrent.Callable<Integer> {
        @Override
        public Integer call() {
            configureLogging();
            return 0;
        }
    }

    @AfterEach
    void resetLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    void setsLogLevelFromCliOption() {
        DummyCommand cmd = new DummyCommand();
        CommandLine cmdLine = new CommandLine(cmd);
        cmdLine.execute("--log-level", "DEBUG");
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertEquals(Level.DEBUG, root.getLevel());
    }

    @Test
    void setsLogLevelFromConfigFile() throws Exception {
        Path cfg = Files.createTempFile("cli", ".properties");
        Files.writeString(cfg, "log.level=WARN");

        DummyCommand cmd = new DummyCommand();
        CommandLine cmdLine = new CommandLine(cmd);
        cmdLine.execute("--config", cfg.toString());
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertEquals(Level.WARN, root.getLevel());
    }
}
