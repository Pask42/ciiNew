package com.cii.messaging.writer;

import com.cii.messaging.model.orderresponse.OrderResponse;

/**
 * JAXB writer for {@link OrderResponse} messages.
 */
public class OrderResponseWriter extends JaxbWriter<OrderResponse> {
    public OrderResponseWriter() {
        super(OrderResponse.class);
    }
}
