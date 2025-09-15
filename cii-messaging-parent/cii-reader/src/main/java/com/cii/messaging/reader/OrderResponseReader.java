package com.cii.messaging.reader;

import com.cii.messaging.model.orderresponse.OrderResponse;

/**
 * JAXB based reader for CrossIndustryOrderResponse documents.
 */
public class OrderResponseReader extends JaxbReader<OrderResponse> {

    public OrderResponseReader() {
        super(OrderResponse.class);
    }
}
