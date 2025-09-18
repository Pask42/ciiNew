package com.cii.messaging.cli;

import com.cii.messaging.validator.ValidationResult;
import com.cii.messaging.validator.ValidationWarning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void invalidSampleReturnsError() throws Exception {
        Path sample = Path.of(getClass().getResource("/order-sample.xml").toURI());
        int exitCode = new CommandLine(new ValidateCommand()).execute(sample.toString());
        assertThat(exitCode).isNotZero();
    }

    @Test
    void validSampleSucceedsWithExplicitVersion() throws Exception {
        Path sample = copyToTemp("order-valid.xml");
        int exitCode = new CommandLine(new ValidateCommand()).execute(
                sample.toString(),
                "--schema-version", "D23B"
        );
        assertThat(exitCode).isZero();
    }

    @Test
    void failOnWarningOptionPropagates() {
        ValidateCommand command = new ValidateCommand();
        setFailOnWarning(command, true);

        ValidationResult result = ValidationResult.builder()
                .valid(true)
                .warnings(List.of(ValidationWarning.builder().message("warning").build()))
                .build();

        assertThat(command.determineExitCode(result)).isEqualTo(1);
    }

    private void setFailOnWarning(ValidateCommand command, boolean value) {
        try {
            var field = ValidateCommand.class.getDeclaredField("failOnWarning");
            field.setAccessible(true);
            field.set(command, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to configure failOnWarning flag for tests", e);
        }
    }

    private Path copyToTemp(String resource) throws IOException, URISyntaxException {
        Path target = tempDir.resolve(resource);
        try (InputStream inputStream = getClass().getResourceAsStream("/" + resource)) {
            if (inputStream == null) {
                throw new IllegalStateException("Resource not found: " + resource);
            }
            Files.copy(inputStream, target);
        }
        return target;
    }
}
