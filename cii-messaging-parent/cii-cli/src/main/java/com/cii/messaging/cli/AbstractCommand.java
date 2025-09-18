package com.cii.messaging.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Classe de base des commandes CLI fournissant la configuration du logging.
 */
public abstract class AbstractCommand {

    @Option(names = {"-l", "--log-level"},
            description = "Niveau de log (ERROR, WARN, INFO, DEBUG, TRACE)")
    private String logLevel;

    @Option(names = {"-c", "--config"},
            description = "Fichier de configuration contenant 'log.level'")
    private Path configFile;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractCommand.class);

    /**
     * Configure le logger racine en fonction des options CLI ou d'un fichier de configuration.
     */
    protected void configureLogging() {
        resolveLogLevel()
                .map(this::convertToLevel)
                .ifPresent(level -> {
                    Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                    root.setLevel(level);
                });
    }

    private Optional<String> resolveLogLevel() {
        if (logLevel != null && !logLevel.isBlank()) {
            return Optional.of(logLevel.trim());
        }

        return loadProperties()
                .map(props -> props.getProperty("log.level"))
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim);
    }

    private Optional<Properties> loadProperties() {
        Properties properties = new Properties();

        if (configFile != null) {
            return loadFromPath(configFile, properties);
        }

        Path cwdConfig = Path.of("cii-cli.properties");
        if (Files.exists(cwdConfig)) {
            Optional<Properties> loaded = loadFromPath(cwdConfig, properties);
            if (loaded.isPresent()) {
                return loaded;
            }
        }

        try (InputStream stream = AbstractCommand.class.getClassLoader().getResourceAsStream("cii-cli.properties")) {
            if (stream != null) {
                properties.load(stream);
                return Optional.of(properties);
            }
        } catch (IOException e) {
            LOGGER.warn("Impossible de lire la configuration par défaut depuis le classpath", e);
        }

        return Optional.empty();
    }

    private Optional<Properties> loadFromPath(Path path, Properties properties) {
        if (!Files.exists(path)) {
            LOGGER.warn("Le fichier de configuration {} n'existe pas", path);
            return Optional.empty();
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            LOGGER.warn("Le fichier de configuration {} n'est pas lisible", path);
            return Optional.empty();
        }

        try (InputStream fis = Files.newInputStream(path)) {
            properties.load(fis);
            return Optional.of(properties);
        } catch (IOException e) {
            LOGGER.warn("Impossible de lire le fichier de configuration {}", path, e);
            return Optional.empty();
        }
    }

    private Level convertToLevel(String configuredLevel) {
        Level level = Level.toLevel(configuredLevel, null);
        if (level == null) {
            LOGGER.warn("Niveau de log '{}' inconnu. Utilisation de INFO par défaut.", configuredLevel);
            return Level.INFO;
        }
        return level;
    }
}
