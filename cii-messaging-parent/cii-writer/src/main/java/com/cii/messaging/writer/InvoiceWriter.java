package com.cii.messaging.writer;

import com.cii.messaging.model.invoice.Invoice;

/**
 * Rédacteur JAXB pour les messages {@link Invoice}.
 */
public class InvoiceWriter extends JaxbWriter<Invoice> {
    public InvoiceWriter() {
        super(Invoice.class);
    }
}
