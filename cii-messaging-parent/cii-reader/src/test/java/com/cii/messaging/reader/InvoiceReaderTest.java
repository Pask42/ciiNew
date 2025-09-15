package com.cii.messaging.reader;

import com.cii.messaging.model.invoice.Invoice;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceReaderTest {

    @Test
    void readsInvoiceSample() throws Exception {
        InvoiceReader reader = new InvoiceReader();
        URL resource = getClass().getResource("/invoice-sample.xml");
        assertNotNull(resource);
        File file = new File(resource.toURI());
        Invoice invoice = reader.read(file);
        assertNotNull(invoice);
    }
}
