package com.cii.messaging.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

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
public class CIIMessagingCLI implements Runnable {
    
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CIIMessagingCLI()).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
