package com.cii.messaging.test.cii.support;

import java.io.InputStream;
import java.util.Objects;

/**
 * Shared helper to retrieve XML examples packaged in {@code cii-samples}.
 */
public final class CiiSampleResource {

    public static final String SAMPLE_FOLDER = "samples/";

    public InputStream open(String sampleFileName) {
        Objects.requireNonNull(sampleFileName, "sampleFileName must not be null");
        var resource = SAMPLE_FOLDER + sampleFileName;
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Unable to locate sample resource: " + resource);
        }
        return stream;
    }
}
