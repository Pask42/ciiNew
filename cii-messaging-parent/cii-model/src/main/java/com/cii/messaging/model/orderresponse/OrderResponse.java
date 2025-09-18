package com.cii.messaging.model.orderresponse;

import com.cii.messaging.unece.orderresponse.CrossIndustryOrderResponseType;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Modèle dédié aux réponses de commande basé sur UNECE CrossIndustryOrderResponseType.
 */
@XmlRootElement(name = "CrossIndustryOrderResponse", namespace = "urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:100")
public class OrderResponse extends CrossIndustryOrderResponseType {
    // Des assistants métiers ou validations complémentaires pourront être ajoutés ici ultérieurement.
}
