package com.cii.messaging.writer.generation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Liste utilitaire des codes UNECE valides pour {@code DocumentStatusCode} dans un ORDER_RESPONSE.
 *
 * <p>Les codes correspondent à la liste officielle « Document Status Code » disponible sur le site de l'UNECE
 * (publication Supply Chain Management &gt; CIOP/CIOR). Cette classe permet de vérifier rapidement qu'un code fourni
 * par l'utilisateur appartient à l'intervalle autorisé (1 à 51 pour D23A/D24A) tout en centralisant la valeur par
 * défaut utilisée par le générateur.</p>
 */
public final class DocumentStatusCodes {

    /** Code indiquant une acceptation de la commande sans modification (valeur par défaut). */
    public static final String DEFAULT_ACKNOWLEDGEMENT_CODE = "29";

    private static final Set<String> VALID_CODES;

    static {
        Set<String> codes = IntStream.rangeClosed(1, 51)
                .mapToObj(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        VALID_CODES = Collections.unmodifiableSet(codes);
    }

    private DocumentStatusCodes() {
        // utilitaire
    }

    /**
     * Indique si le code fourni figure dans la liste officielle UNECE.
     *
     * @param code valeur à tester (peut être {@code null})
     * @return {@code true} si le code est reconnu, sinon {@code false}
     */
    public static boolean isValid(String code) {
        return code != null && VALID_CODES.contains(code);
    }

    /**
     * Retourne la liste immuable des codes acceptés afin d'afficher un message d'erreur explicite.
     *
     * @return ensemble trié des codes valides
     */
    public static Set<String> validCodes() {
        return VALID_CODES;
    }
}
