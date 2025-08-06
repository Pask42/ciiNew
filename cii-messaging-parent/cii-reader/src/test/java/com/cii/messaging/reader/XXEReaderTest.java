package com.cii.messaging.reader;

import com.cii.messaging.reader.CIIReaderException;
import com.cii.messaging.reader.impl.DesadvReader;
import com.cii.messaging.reader.impl.InvoiceReader;
import com.cii.messaging.reader.impl.OrderReader;
import com.cii.messaging.reader.impl.OrderResponseReader;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;
import javax.xml.stream.XMLStreamException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class XXEReaderTest {
    private static final String XXE = """
            <?xml version=\"1.0\"?>
            <!DOCTYPE foo [<!ELEMENT foo ANY><!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>
            <foo>&xxe;</foo>
            """;

    private File createTempXml() throws IOException {
        File temp = File.createTempFile("xxe", ".xml");
        Files.writeString(temp.toPath(), XXE);
        return temp;
    }

    @Test
    void orderReaderRejectsExternalEntities() throws Exception {
        OrderReader reader = new OrderReader();
        File file = createTempXml();
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> reader.read(file));
        assertTrue(ex.getCause() instanceof SAXParseException);
    }

    @Test
    void invoiceReaderRejectsExternalEntities() throws Exception {
        InvoiceReader reader = new InvoiceReader();
        File file = createTempXml();
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> reader.read(file));
        assertTrue(ex.getCause() instanceof SAXParseException);
    }

    @Test
    void invoiceReaderRejectsExternalEntitiesFromStream() {
        InvoiceReader reader = new InvoiceReader();
        InputStream is = new ByteArrayInputStream(XXE.getBytes(StandardCharsets.UTF_8));
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> reader.read(is));
        assertTrue(ex.getCause() instanceof SAXParseException);
    }

    @Test
    void desadvReaderRejectsExternalEntities() throws Exception {
        DesadvReader reader = new DesadvReader();
        File file = createTempXml();
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> reader.read(file));
        assertTrue(ex.getCause() instanceof SAXParseException);
    }

    @Test
    void orderResponseReaderRejectsExternalEntities() throws Exception {
        OrderResponseReader reader = new OrderResponseReader();
        File file = createTempXml();
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> reader.read(file));
        assertTrue(ex.getCause() instanceof SAXParseException);
    }

    @Test
    void factoryRejectsExternalEntities() {
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> CIIReaderFactory.createReader(XXE));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof SAXParseException || cause instanceof XMLStreamException,
                "Unexpected cause type: " + (cause == null ? "null" : cause.getClass().getName()));
    }

    @Test
    void factoryRejectsExternalEntitiesFromFile() throws Exception {
        File file = createTempXml();
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> CIIReaderFactory.createReader(file.toPath()));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof SAXParseException || cause instanceof XMLStreamException,
                "Unexpected cause type: " + (cause == null ? "null" : cause.getClass().getName()));
    }
}
