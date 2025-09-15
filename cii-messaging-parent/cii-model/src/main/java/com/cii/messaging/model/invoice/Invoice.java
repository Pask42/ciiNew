package com.cii.messaging.model.invoice;

import com.cii.messaging.unece.invoice.CrossIndustryInvoiceType;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Dedicated model for invoices based on UNECE CrossIndustryInvoiceType.
 */
@XmlRootElement(name = "CrossIndustryInvoice", namespace = "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100")
public class Invoice extends CrossIndustryInvoiceType {
    // Additional domain-specific helpers or validations can be added here later.
}
