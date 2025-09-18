package com.cii.messaging.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ParseCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesSampleOrderToSummaryFile() throws Exception {
        Path sample = Path.of(getClass().getResource("/order-sample.xml").toURI());
        Path output = tempDir.resolve("summary.txt");

        int exitCode = new CommandLine(new ParseCommand()).execute(
                sample.toString(),
                "--output", output.toString()
        );

        assertThat(exitCode).isZero();
        String content = Files.readString(output);
        assertThat(content)
                .contains("Commande")
                .contains("ORD-2024-001");
    }

    @Test
    void writesJsonWhenRequested() throws Exception {
        Path sample = Path.of(getClass().getResource("/order-sample.xml").toURI());
        Path output = tempDir.resolve("order.json");

        int exitCode = new CommandLine(new ParseCommand()).execute(
                "--format", "JSON",
                sample.toString(),
                "--output", output.toString()
        );

        assertThat(exitCode).isZero();
        String json = Files.readString(output);
        assertThat(json).contains("ORD-2024-001");
    }

    @Test
    void missingFileReturnsError() {
        Path missing = tempDir.resolve("missing.xml");
        int exitCode = new CommandLine(new ParseCommand()).execute(missing.toString());
        assertThat(exitCode).isNotZero();
    }
}
