package com.cii.messaging.reader;

import java.io.File;
import java.io.InputStream;

/**
 * Contrat générique de lecture pour transformer un contenu XML en modèles métier.
 *
 * @param <T> type de modèle produit par ce lecteur
 */
public interface CIIReader<T> {
    T read(File xmlFile) throws CIIReaderException;
    T read(InputStream inputStream) throws CIIReaderException;
    T read(String xmlContent) throws CIIReaderException;
}
