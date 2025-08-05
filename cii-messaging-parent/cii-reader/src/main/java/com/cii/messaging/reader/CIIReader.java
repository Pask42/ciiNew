package com.cii.messaging.reader;

import com.cii.messaging.model.CIIMessage;
import java.io.File;
import java.io.InputStream;

public interface CIIReader {
    CIIMessage read(File xmlFile) throws CIIReaderException;
    CIIMessage read(InputStream inputStream) throws CIIReaderException;
    CIIMessage read(String xmlContent) throws CIIReaderException;
}
