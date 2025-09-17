package com.cii.messaging.test.cii.support;

import java.io.InputStream;
import java.util.Objects;

/**
 * Lightweight helper exposing access to the XML fixtures published by the
 * {@code cii-samples} module.  Tests can load resources by their simple file
 * name without having to repeat classpath boilerplate.
 */
public final class CiiSampleResource {

    /** Base folder inside {@code cii-samples} where XML examples are stored. */
    public static final String SAMPLE_FOLDER = "samples/";

    /**
     * Opens a resource stream pointing to the requested sample XML file.
     *
     * @param sampleFileName the simple file name (e.g. {@code invoice-sample.xml})
     * @return a non-null input stream ready to be consumed by the caller
     * @throws IllegalArgumentException if the resource cannot be located on the classpath
     */
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
