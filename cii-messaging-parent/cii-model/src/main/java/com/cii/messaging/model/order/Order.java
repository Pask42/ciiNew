package com.cii.messaging.model.order;

import com.cii.messaging.unece.order.CrossIndustryOrderType;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Modèle dédié aux commandes basé sur UNECE CrossIndustryOrderType.
 */
@XmlRootElement(name = "CrossIndustryOrder", namespace = "urn:un:unece:uncefact:data:standard:CrossIndustryOrder:100")
public class Order extends CrossIndustryOrderType {
    // Des assistants métiers ou validations complémentaires pourront être ajoutés ici ultérieurement.
}
