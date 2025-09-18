package com.cii.messaging.writer;

import com.cii.messaging.model.order.Order;

/**
 * Rédacteur JAXB pour les messages {@link Order}.
 */
public class OrderWriter extends JaxbWriter<Order> {
    public OrderWriter() {
        super(Order.class);
    }
}
