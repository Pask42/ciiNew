package com.cii.messaging.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Optional;

@Command(
    name = "cii-cli",
    mixinStandardHelpOptions = true,
    versionProvider = CIIMessagingCLI.ManifestVersionProvider.class,
    description = {
            "Outil en ligne de commande pour traiter les messages CII",
            "Parse et valide les documents ORDER, ORDERSP, DESADV et INVOICE"
    },
    subcommands = {
        ParseCommand.class,
        ValidateCommand.class,
        RespondCommand.class
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

    /**
     * Lit la version de la CLI depuis le manifeste jar alimenté par Maven lors du build.
     */
    static class ManifestVersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            String implementationVersion = Optional
                    .ofNullable(CIIMessagingCLI.class.getPackage())
                    .map(Package::getImplementationVersion)
                    .orElse("(development)");
            return new String[]{"CII CLI " + implementationVersion};
        }
    }
}
