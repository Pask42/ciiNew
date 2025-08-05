package com.cii.messaging.writer;

import com.cii.messaging.model.CIIMessage;
import java.io.File;
import java.io.OutputStream;

public interface CIIWriter {
    void write(CIIMessage message, File outputFile) throws CIIWriterException;
    void write(CIIMessage message, OutputStream outputStream) throws CIIWriterException;
    String writeToString(CIIMessage message) throws CIIWriterException;
    void setFormatOutput(boolean format);
    void setEncoding(String encoding);
}
