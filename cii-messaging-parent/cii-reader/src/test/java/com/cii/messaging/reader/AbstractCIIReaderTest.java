package com.cii.messaging.reader;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.reader.impl.AbstractCIIReader;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AbstractCIIReaderTest {

    private static class TestReader extends AbstractCIIReader {
        @Override
        protected void initializeJAXBContext() throws JAXBException {
            jaxbContext = JAXBContext.newInstance(Object.class);
        }

        @Override
        protected CIIMessage parseDocument(Object document) {
            return null;
        }

        BigDecimal parse(String value) {
            return parseBigDecimal(value);
        }
    }

    @Test
    void returnsNullForInvalidNumbers() {
        TestReader reader = new TestReader();
        assertNull(reader.parse("abc"));
        assertNull(reader.parse(null));
        assertEquals(new BigDecimal("12.3"), reader.parse("12.3"));
    }
}
