package com.cii.messaging.writer.generation;

import com.cii.messaging.model.invoice.Invoice;

/**
 * Représente une facture métier capable de produire un message {@link Invoice} prêt à être sérialisé.
 */
@FunctionalInterface
public interface ObjetInvoice {

    /**
     * Construit l'instance {@link Invoice} correspondant à la facture.
     *
     * @return le message {@link Invoice} prêt à être marshalled
     */
    Invoice toInvoice();
}
