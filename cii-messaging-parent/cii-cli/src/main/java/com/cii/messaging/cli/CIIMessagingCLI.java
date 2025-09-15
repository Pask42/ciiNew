package com.cii.messaging.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(
    name = "cii-cli",
    mixinStandardHelpOptions = true,
    version = "CII CLI 1.0.0",
    description = "Outil en ligne de commande pour traiter les messages CII",
    subcommands = {
        ParseCommand.class,
        ValidateCommand.class
    }
)
public class CIIMessagingCLI extends AbstractCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CIIMessagingCLI.class);

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CIIMessagingCLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        configureLogging();
        logger.info("Aucune commande spécifiée. Affichage de l'aide.");
        spec.commandLine().usage(System.out);
    }
}
