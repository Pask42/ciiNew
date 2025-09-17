package com.cii.messaging.writer.generation;

import com.cii.messaging.model.despatchadvice.DespatchAdvice;

/**
 * Représente un avis d'expédition métier capable de produire un message {@link DespatchAdvice} prêt à être sérialisé.
 */
@FunctionalInterface
public interface ObjetDesadv {

    /**
     * Construit l'instance {@link DespatchAdvice} correspondant à l'avis d'expédition.
     *
     * @return le message {@link DespatchAdvice} prêt à être marshalled
     */
    DespatchAdvice toDespatchAdvice();
}
