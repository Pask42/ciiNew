package com.cii.messaging.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ParseCommandTest {

    @Test
    void testInvalidFormat() {
        CommandLine cmd = new CommandLine(new ParseCommand());
        int exitCode = cmd.execute("--format", "XML", "dummy.xml");
        assertNotEquals(0, exitCode);
    }

    @Test
    void testUnreadableFile() throws Exception {
        ParseCommand command = new ParseCommand();
        Field inputField = ParseCommand.class.getDeclaredField("inputFile");
        inputField.setAccessible(true);
        File fake = new File("dummy") {
            @Override
            public boolean exists() { return true; }
            @Override
            public boolean isFile() { return true; }
            @Override
            public boolean canRead() { return false; }
        };
        inputField.set(command, fake);
        Integer result = command.call();
        assertEquals(1, result.intValue());
    }
}
