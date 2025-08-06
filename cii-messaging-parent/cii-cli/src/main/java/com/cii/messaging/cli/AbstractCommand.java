package com.cii.messaging.cli;

import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for CLI commands providing logging configuration support.
 */
public abstract class AbstractCommand {

    @Option(names = {"-l", "--log-level"},
            description = "Log level (ERROR, WARN, INFO, DEBUG, TRACE)")
    private String logLevel;

    @Option(names = {"-c", "--config"},
            description = "Configuration file containing 'log.level'")
    private File configFile;

    /**
     * Configures the root logger based on CLI options or configuration file.
     */
    protected void configureLogging() {
        String level = logLevel;

        File cfgFile = configFile != null ? configFile : new File("cii-cli.properties");
        if (level == null && cfgFile.exists()) {
            try (FileInputStream fis = new FileInputStream(cfgFile)) {
                Properties props = new Properties();
                props.load(fis);
                level = props.getProperty("log.level");
            } catch (IOException e) {
                org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
                logger.warn("Unable to read config file {}", cfgFile, e);
            }
        }

        if (level != null) {
            Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.toLevel(level, Level.INFO));
        }
    }
}

