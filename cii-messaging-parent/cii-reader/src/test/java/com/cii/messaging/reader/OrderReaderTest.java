package com.cii.messaging.reader;

import com.cii.messaging.model.order.Order;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class OrderReaderTest {

    @Test
    void litEchantillonOrder() throws Exception {
        OrderReader reader = new OrderReader();
        URL resource = getClass().getResource("/order-sample.xml");
        assertNotNull(resource);
        File file = new File(resource.toURI());
        Order order = reader.read(file);
        assertNotNull(order);
    }
}
