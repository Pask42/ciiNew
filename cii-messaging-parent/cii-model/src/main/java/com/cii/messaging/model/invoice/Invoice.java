package com.cii.messaging.model.invoice;

import com.cii.messaging.unece.invoice.CrossIndustryInvoiceType;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Modèle dédié aux factures basé sur UNECE CrossIndustryInvoiceType.
 */
@XmlRootElement(name = "CrossIndustryInvoice", namespace = "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100")
public class Invoice extends CrossIndustryInvoiceType {
    // Des assistants métiers ou validations complémentaires pourront être ajoutés ici ultérieurement.
}
