package com.cii.messaging.reader;

import com.cii.messaging.model.despatchadvice.DespatchAdvice;

/**
 * JAXB based reader for CrossIndustryDespatchAdvice documents.
 */
public class DesadvReader extends JaxbReader<DespatchAdvice> {

    public DesadvReader() {
        super(DespatchAdvice.class);
    }
}
