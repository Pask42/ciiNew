package com.cii.messaging.writer;

import java.io.File;
import java.io.OutputStream;

/**
 * Generic writer able to marshal CII message models using JAXB.
 *
 * @param <T> message model type
 */
public interface CIIWriter<T> {
    void write(T message, File outputFile) throws CIIWriterException;
    void write(T message, OutputStream outputStream) throws CIIWriterException;
    String writeToString(T message) throws CIIWriterException;
    void setFormatOutput(boolean format);
    void setEncoding(String encoding);
}
