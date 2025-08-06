package com.cii.messaging.validator.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import com.cii.messaging.reader.CIIReader;
import com.cii.messaging.reader.CIIReaderFactory;
import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class XSDValidatorTest {

    @Test
    void validateCIIMessageValid() throws Exception {
        String xml = Files.readString(Path.of("src", "test", "resources", "invoice-sample.xml"));
        CIIReader reader = CIIReaderFactory.createReader(MessageType.INVOICE);
        CIIMessage message = reader.read(xml);
        XSDValidator validator = new XSDValidator();
        ValidationResult result = validator.validate(message);
        assertTrue(result.isValid(), result.getErrors().toString());
    }

    @Test
    void validateCIIMessageInvalid() {
        CIIMessage message = CIIMessage.builder()
                .messageId("INV-1")
                .messageType(MessageType.INVOICE)
                .creationDateTime(LocalDateTime.now())
                .build();
        XSDValidator validator = new XSDValidator();
        ValidationResult result = validator.validate(message);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void validateConcurrentAccess() throws Exception {
        Path path = Path.of("src", "test", "resources", "invoice-sample.xml");
        File file = path.toFile();
        XSDValidator validator = new XSDValidator();

        int threads = 10;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        List<Callable<ValidationResult>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> validator.validate(file));
        }
        List<Future<ValidationResult>> results = service.invokeAll(tasks);
        for (Future<ValidationResult> future : results) {
            ValidationResult res = future.get();
            assertTrue(res.isValid(), res.getErrors().toString());
        }
        service.shutdown();
    }

    @Test
    void validateLargeFile() throws Exception {
        Path sample = Path.of("src", "test", "resources", "invoice-sample.xml");
        Path large = Files.createTempFile("invoice-large", ".xml");

        try (BufferedReader reader = Files.newBufferedReader(sample);
             BufferedWriter writer = Files.newBufferedWriter(large)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("</rsm:CrossIndustryInvoice>")) {
                    for (int i = 0; i < 200000; i++) {
                        writer.write("<!-- comment -->\n");
                    }
                }
                writer.write(line);
                writer.newLine();
            }
        }

        assertTrue(Files.size(large) > 1_000_000); // ensure file is reasonably large (>1MB)

        XSDValidator validator = new XSDValidator();
        ValidationResult result = validator.validate(large.toFile());
        assertTrue(result.isValid(), result.getErrors().toString());

        Files.deleteIfExists(large);
    }
}
