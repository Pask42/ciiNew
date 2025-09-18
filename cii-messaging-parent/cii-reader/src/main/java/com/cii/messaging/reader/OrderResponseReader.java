package com.cii.messaging.reader;

import com.cii.messaging.model.orderresponse.OrderResponse;

/**
 * Lecteur basé sur JAXB pour les documents CrossIndustryOrderResponse.
 */
public class OrderResponseReader extends JaxbReader<OrderResponse> {

    public OrderResponseReader() {
        super(OrderResponse.class);
    }
}
