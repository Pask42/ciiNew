package com.cii.messaging.writer;

import com.cii.messaging.model.order.Order;

/**
 * JAXB writer for {@link Order} messages.
 */
public class OrderWriter extends JaxbWriter<Order> {
    public OrderWriter() {
        super(Order.class);
    }
}
