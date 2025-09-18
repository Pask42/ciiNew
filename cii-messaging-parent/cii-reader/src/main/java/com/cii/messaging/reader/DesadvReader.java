package com.cii.messaging.reader;

import com.cii.messaging.model.despatchadvice.DespatchAdvice;

/**
 * Lecteur basé sur JAXB pour les documents CrossIndustryDespatchAdvice.
 */
public class DesadvReader extends JaxbReader<DespatchAdvice> {

    public DesadvReader() {
        super(DespatchAdvice.class);
    }
}
