package com.cii.messaging.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCommandTest {

    @Test
    void failsWhenOrderFileIsNotReadableFile() throws IOException {
        Path tempDir = Files.createTempDirectory("orderDir");
        Path output = Files.createTempFile("out", ".xml");
        CommandLine cmd = new CommandLine(new GenerateCommand());
        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));
        int exit = cmd.execute("INVOICE", "-o", output.toString(), "--from-order", tempDir.toString());
        assertEquals(1, exit);
        assertTrue(err.toString().contains("lisible"));
    }

    @Test
    void failsWhenCannotCreateOutputDirectories() throws IOException {
        Path root = Files.createTempDirectory("root");
        Path file = Files.createTempFile(root, "file", ".txt");
        Path output = file.resolve("subdir/out.xml");
        CommandLine cmd = new CommandLine(new GenerateCommand());
        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));
        int exit = cmd.execute("INVOICE", "-o", output.toString());
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Impossible de créer les répertoires"));
    }

    @Test
    void failsWhenWritingToDirectory() throws IOException {
        Path dir = Files.createTempDirectory("outdir");
        CommandLine cmd = new CommandLine(new GenerateCommand());
        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));
        int exit = cmd.execute("INVOICE", "-o", dir.toString());
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Impossible d'écrire le fichier de sortie"));
    }

    @Test
    void generatesJsonOutput() throws IOException {
        Path output = Files.createTempFile("message", ".json");
        CommandLine cmd = new CommandLine(new GenerateCommand());
        int exit = cmd.execute("INVOICE", "-o", output.toString(), "--format", "JSON");
        assertEquals(0, exit);
        assertTrue(Files.size(output) > 0);
    }
}
