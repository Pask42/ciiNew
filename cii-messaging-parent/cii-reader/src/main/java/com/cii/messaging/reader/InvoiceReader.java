package com.cii.messaging.reader;

import com.cii.messaging.model.invoice.Invoice;

/**
 * Lecteur basé sur JAXB pour les documents CrossIndustryInvoice.
 */
public class InvoiceReader extends JaxbReader<Invoice> {

    public InvoiceReader() {
        super(Invoice.class);
    }
}
