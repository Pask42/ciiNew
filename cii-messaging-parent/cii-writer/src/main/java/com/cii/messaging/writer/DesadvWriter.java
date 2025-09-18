package com.cii.messaging.writer;

import com.cii.messaging.model.despatchadvice.DespatchAdvice;

/**
 * Rédacteur JAXB pour les messages {@link DespatchAdvice}.
 */
public class DesadvWriter extends JaxbWriter<DespatchAdvice> {
    public DesadvWriter() {
        super(DespatchAdvice.class);
    }
}
