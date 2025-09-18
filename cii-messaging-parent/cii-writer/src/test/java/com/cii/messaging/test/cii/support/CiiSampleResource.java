package com.cii.messaging.test.cii.support;

import java.io.InputStream;
import java.util.Objects;

/**
 * Utilitaire pour accéder aux exemples XML fournis par le module {@code cii-samples}.
 */
public final class CiiSampleResource {

    public static final String SAMPLE_FOLDER = "samples/";

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
