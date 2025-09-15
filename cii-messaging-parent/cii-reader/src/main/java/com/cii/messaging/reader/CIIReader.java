package com.cii.messaging.reader;

import java.io.File;
import java.io.InputStream;

/**
 * Generic reader contract for converting XML content into domain message models.
 *
 * @param <T> type of the message model produced by this reader
 */
public interface CIIReader<T> {
    T read(File xmlFile) throws CIIReaderException;
    T read(InputStream inputStream) throws CIIReaderException;
    T read(String xmlContent) throws CIIReaderException;
}
