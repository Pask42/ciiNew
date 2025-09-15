package com.cii.messaging.writer;

import com.cii.messaging.model.despatchadvice.DespatchAdvice;

/**
 * JAXB writer for {@link DespatchAdvice} messages.
 */
public class DesadvWriter extends JaxbWriter<DespatchAdvice> {
    public DesadvWriter() {
        super(DespatchAdvice.class);
    }
}
