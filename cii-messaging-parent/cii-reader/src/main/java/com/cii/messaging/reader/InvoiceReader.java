package com.cii.messaging.reader;

import com.cii.messaging.model.invoice.Invoice;

/**
 * JAXB based reader for CrossIndustryInvoice documents.
 */
public class InvoiceReader extends JaxbReader<Invoice> {

    public InvoiceReader() {
        super(Invoice.class);
    }
}
