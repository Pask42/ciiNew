package com.cii.messaging.reader;

import com.cii.messaging.reader.CIIReaderException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class XXEReaderTest {
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
    void fabriqueRejetteEntitesExternes() {
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> CIIReaderFactory.createReader(XXE));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof SAXParseException || cause instanceof XMLStreamException,
                "Type de cause inattendu : " + (cause == null ? "null" : cause.getClass().getName()));
    }

    @Test
    void fabriqueRejetteEntitesExternesDepuisFichier() throws Exception {
        File file = createTempXml();
        CIIReaderException ex = assertThrows(CIIReaderException.class, () -> CIIReaderFactory.createReader(file.toPath()));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof SAXParseException || cause instanceof XMLStreamException,
                "Type de cause inattendu : " + (cause == null ? "null" : cause.getClass().getName()));
    }
}
