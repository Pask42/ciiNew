package com.cii.messaging.reader;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.reader.impl.OrderReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Currency;

public class OrderReaderTest {

    @Test
    void extractsCurrencyFromOrder() throws Exception {
        OrderReader reader = new OrderReader();
        URL resource = getClass().getResource("/order-sample.xml");
        assertNotNull(resource);
        File file = new File(resource.toURI());
        CIIMessage message = reader.read(file);
          assertNotNull(message.getHeader());
          assertEquals(Currency.getInstance("EUR"), message.getHeader().getCurrency());
      }

    @Test
    void extractsLineTotalFromHeader() throws Exception {
        OrderReader reader = new OrderReader();
        URL resource = getClass().getResource("/order-with-header.xml");
        assertNotNull(resource);
        File file = new File(resource.toURI());
        CIIMessage message = reader.read(file);
        assertNotNull(message.getTotals());
        assertEquals(new BigDecimal("25000.00"), message.getTotals().getLineTotalAmount());
    }
}
