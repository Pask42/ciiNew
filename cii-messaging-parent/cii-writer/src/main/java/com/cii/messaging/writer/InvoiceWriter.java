package com.cii.messaging.writer;

import com.cii.messaging.model.invoice.Invoice;

/**
 * JAXB writer for {@link Invoice} messages.
 */
public class InvoiceWriter extends JaxbWriter<Invoice> {
    public InvoiceWriter() {
        super(Invoice.class);
    }
}
