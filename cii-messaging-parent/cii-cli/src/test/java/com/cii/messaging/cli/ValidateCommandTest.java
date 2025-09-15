package com.cii.messaging.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidateCommandTest {

    @Test
    void invalidSampleReturnsError() {
        String sample = getClass().getResource("/order-invalid-sample.xml").getPath();
        int exitCode = new CommandLine(new ValidateCommand()).execute(sample);
        assertThat(exitCode).isNotZero();
    }
}
