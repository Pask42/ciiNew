package com.cii.messaging.writer.generation;

import com.cii.messaging.model.order.Order;

/**
 * Représente une commande métier capable de produire un message {@link Order} prêt à être sérialisé.
 */
@FunctionalInterface
public interface ObjetCommande {

    /**
     * Construit l'instance {@link Order} correspondant à la commande.
     *
     * @return le message {@link Order} prêt à être marshalled
     */
    Order toOrder();
}
