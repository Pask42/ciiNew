package com.cii.messaging.test.cii.support;

import java.io.InputStream;
import java.util.Objects;

/**
 * Petit utilitaire donnant accès aux jeux d'essai XML publiés par le module
 * {@code cii-samples}. Les tests peuvent charger une ressource à partir de son
 * nom de fichier sans répéter le code standard de parcours du classpath.
 */
public final class CiiSampleResource {

    /** Dossier racine dans {@code cii-samples} où sont stockés les exemples XML. */
    public static final String SAMPLE_FOLDER = "samples/";

    /**
     * Ouvre un flux pointant vers le fichier XML d'exemple demandé.
     *
     * @param sampleFileName nom simple du fichier (ex. {@code invoice-sample.xml})
     * @return un flux non nul prêt à être consommé par l'appelant
     * @throws IllegalArgumentException si la ressource est introuvable sur le classpath
     */
    public InputStream open(String sampleFileName) {
        Objects.requireNonNull(sampleFileName, "sampleFileName ne doit pas être null");
        var resource = SAMPLE_FOLDER + sampleFileName;
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Impossible de localiser la ressource d'exemple : " + resource);
        }
        return stream;
    }
}
