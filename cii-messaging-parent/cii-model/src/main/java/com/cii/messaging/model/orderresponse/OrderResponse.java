package com.cii.messaging.model.orderresponse;

import com.cii.messaging.unece.orderresponse.CrossIndustryOrderResponseType;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Dedicated model for order responses based on UNECE CrossIndustryOrderResponseType.
 */
@XmlRootElement(name = "CrossIndustryOrderResponse")
public class OrderResponse extends CrossIndustryOrderResponseType {
    // Additional domain-specific helpers or validations can be added here later.
}
