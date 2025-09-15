package com.cii.messaging.model.order;

import com.cii.messaging.unece.order.CrossIndustryOrderType;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Dedicated model for orders based on UNECE CrossIndustryOrderType.
 */
@XmlRootElement(name = "CrossIndustryOrder", namespace = "urn:un:unece:uncefact:data:standard:CrossIndustryOrder:16")
public class Order extends CrossIndustryOrderType {
    // Additional domain-specific helpers or validations can be added here later.
}
