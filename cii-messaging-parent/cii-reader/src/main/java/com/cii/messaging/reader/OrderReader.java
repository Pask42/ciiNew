package com.cii.messaging.reader;

import com.cii.messaging.model.order.Order;

/**
 * JAXB based reader for CrossIndustryOrder documents.
 */
public class OrderReader extends JaxbReader<Order> {

    public OrderReader() {
        super(Order.class);
    }
}
