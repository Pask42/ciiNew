package com.cii.messaging.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ValidateCommandTest {

    @Test
    void directoryIsRejected(@TempDir Path tempDir) {
        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cli.setErr(new PrintWriter(err, true));

        int exitCode = cli.execute(tempDir.toString());

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("N'est pas un fichier : " + tempDir.toFile());
    }

    @Test
    void unreadableFileIsRejected(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.xml");
        Files.writeString(file, "<xml/>");

        try {
            Files.setPosixFilePermissions(file, Set.of());
        } catch (UnsupportedOperationException e) {
            // skip test on filesystems that do not support POSIX permissions
            assumeTrue(false);
        }
        assumeTrue(!Files.isReadable(file));

        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cli.setErr(new PrintWriter(err, true));

        int exitCode = cli.execute(file.toString());

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("Impossible de lire le fichier : " + file.toFile());
    }

    @Test
    void invalidSchemaVersionIsReported(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("valid.xml");
        Files.writeString(file, "<xml/>");

        ValidateCommand cmd = new ValidateCommand();
        CommandLine cli = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cli.setErr(new PrintWriter(err, true));

        int exitCode = cli.execute("--schema", "foo", file.toString());

        assertThat(exitCode).isNotEqualTo(0);
        assertThat(err.toString()).contains("Invalid value for option '--schema'");
    }
}
