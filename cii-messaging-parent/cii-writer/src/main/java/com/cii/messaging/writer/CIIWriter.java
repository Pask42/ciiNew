package com.cii.messaging.writer;

import java.io.File;
import java.io.OutputStream;

/**
 * Rédacteur générique capable de sérialiser des modèles CII en XML via JAXB.
 *
 * @param <T> type de modèle manipulé
 */
public interface CIIWriter<T> {
    void write(T message, File outputFile) throws CIIWriterException;
    void write(T message, OutputStream outputStream) throws CIIWriterException;
    String writeToString(T message) throws CIIWriterException;
    void setFormatOutput(boolean format);
    void setEncoding(String encoding);
}
