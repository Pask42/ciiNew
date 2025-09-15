package com.cii.messaging.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

public class ParseCommandTest {

    @Test
    void parsesSampleOrder() {
        String sample = getClass().getResource("/order-sample.xml").getPath();
        int exitCode = new CommandLine(new ParseCommand()).execute(sample);
        assertThat(exitCode).isZero();
    }
}
