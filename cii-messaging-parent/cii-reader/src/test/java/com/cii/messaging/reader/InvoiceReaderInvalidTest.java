package com.cii.messaging.reader;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.reader.impl.InvoiceReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceReaderInvalidTest {

    @Test
    void handlesInvalidNumericValues() throws Exception {
        InvoiceReader reader = new InvoiceReader();
        URL resource = getClass().getResource("/invoice-invalid-numeric.xml");
        assertNotNull(resource);
        File file = new File(resource.toURI());
        CIIMessage message = reader.read(file);
        assertNotNull(message.getTotals());
        assertEquals(new BigDecimal("15000.00"), message.getTotals().getLineTotalAmount());
        assertNull(message.getTotals().getTaxBasisAmount());
    }
}
