package com.cii.messaging.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(
    name = "cii-messaging",
    mixinStandardHelpOptions = true,
    version = "CII Messaging CLI 1.0.0",
    description = "Command line tool for CII message processing",
    subcommands = {
        GenerateCommand.class,
        ParseCommand.class,
        ValidateCommand.class,
        ConvertCommand.class
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
        logger.info("No command specified. Showing usage.");
        spec.commandLine().usage(System.out);
    }
}
