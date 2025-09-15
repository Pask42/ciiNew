package com.cii.messaging.model.despatchadvice;

import com.cii.messaging.unece.despatchadvice.CrossIndustryDespatchAdviceType;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Dedicated model for despatch advice based on UNECE CrossIndustryDespatchAdviceType.
 */
@XmlRootElement(name = "CrossIndustryDespatchAdvice", namespace = "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:100")
public class DespatchAdvice extends CrossIndustryDespatchAdviceType {
    // Additional domain-specific helpers or validations can be added here later.
}
