package com.cii.messaging.reader;

import com.cii.messaging.model.order.Order;

/**
 * Lecteur basé sur JAXB pour les documents CrossIndustryOrder.
 */
public class OrderReader extends JaxbReader<Order> {

    public OrderReader() {
        super(Order.class);
    }
}
