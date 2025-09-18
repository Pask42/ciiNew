package com.cii.messaging.model.despatchadvice;

import com.cii.messaging.unece.despatchadvice.CrossIndustryDespatchAdviceType;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Modèle dédié aux avis d'expédition basé sur UNECE CrossIndustryDespatchAdviceType.
 */
@XmlRootElement(name = "CrossIndustryDespatchAdvice", namespace = "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:100")
public class DespatchAdvice extends CrossIndustryDespatchAdviceType {
    // Des assistants métiers ou validations complémentaires pourront être ajoutés ici ultérieurement.
}
