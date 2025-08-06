package com.cii.messaging.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

public class ConvertCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void failsWhenInputAlreadyJson() throws Exception {
        Path input = tempDir.resolve("input.json");
        Files.writeString(input, "{}");
        Path output = tempDir.resolve("out.json");

        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        try {
            int exitCode = new CommandLine(new ConvertCommand())
                    .execute(input.toString(), "-o", output.toString(), "-t", "JSON");
            assertEquals(1, exitCode);
            assertTrue(err.toString().contains("Input is already JSON"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void failsWhenInputAlreadyXml() throws Exception {
        Path input = tempDir.resolve("input.xml");
        Files.writeString(input, "<root/>");
        Path output = tempDir.resolve("out.xml");

        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        try {
            int exitCode = new CommandLine(new ConvertCommand())
                    .execute(input.toString(), "-o", output.toString(), "-t", "XML");
            assertEquals(1, exitCode);
            assertTrue(err.toString().contains("Input is already XML"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void failsWhenInputMalformed() throws Exception {
        Path input = tempDir.resolve("input.txt");
        Files.writeString(input, "not json or xml");
        Path output = tempDir.resolve("out.json");

        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        try {
            int exitCode = new CommandLine(new ConvertCommand())
                    .execute(input.toString(), "-o", output.toString(), "-t", "JSON");
            assertEquals(1, exitCode);
            assertTrue(err.toString().contains("neither valid JSON nor XML"));
        } finally {
            System.setErr(originalErr);
        }
    }
}
