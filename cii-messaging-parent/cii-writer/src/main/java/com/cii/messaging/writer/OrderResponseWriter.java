package com.cii.messaging.writer;

import com.cii.messaging.model.orderresponse.OrderResponse;

/**
 * Rédacteur JAXB pour les messages {@link OrderResponse}.
 */
public class OrderResponseWriter extends JaxbWriter<OrderResponse> {
    public OrderResponseWriter() {
        super(OrderResponse.class);
    }
}
