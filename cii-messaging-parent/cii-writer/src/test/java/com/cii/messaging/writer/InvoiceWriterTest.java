package com.cii.messaging.writer;

import com.cii.messaging.model.invoice.Invoice;
import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceWriterTest {

    @Test
    void shouldWriteInvoice() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/invoice-sample.xml")) {
            Invoice invoice = (Invoice) JAXBContext.newInstance(Invoice.class)
                    .createUnmarshaller().unmarshal(is);
            CIIWriter<Invoice> writer = new InvoiceWriter();
            String xml = writer.writeToString(invoice);
            assertTrue(xml.contains("CrossIndustryInvoice"));
        }
    }
}
