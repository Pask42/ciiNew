package com.cii.messaging.writer;

import com.cii.messaging.model.order.Order;
import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderWriterTest {

    @Test
    void shouldWriteOrder() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/order-sample.xml")) {
            Order order = (Order) JAXBContext.newInstance(Order.class)
                    .createUnmarshaller().unmarshal(is);
            CIIWriter<Order> writer = new OrderWriter();
            String xml = writer.writeToString(order);
            assertTrue(xml.contains("CrossIndustryOrder"));
        }
    }
}
