package com.cii.messaging.reader;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.Address;
import com.cii.messaging.reader.impl.InvoiceReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class InvoiceReaderTest {

    @Test
    void extractsTradePartiesAndTotals() throws Exception {
        InvoiceReader reader = new InvoiceReader();
        URL resource = getClass().getResource("/invoice-sample.xml");
        assertNotNull(resource);
        File file = new File(resource.toURI());
        CIIMessage message = reader.read(file);
        assertNotNull(message.getTotals());
        assertEquals(new BigDecimal("15000.00"), message.getTotals().getTaxBasisAmount());
        assertNotNull(message.getSeller());
        assertEquals("Seller Company GmbH", message.getSeller().getName());
        Address sellerAddr = message.getSeller().getAddress();
        assertNotNull(sellerAddr);
        assertEquals("Berlin", sellerAddr.getCity());
        assertNotNull(message.getSeller().getContact());
        assertEquals("DE123456789", message.getSeller().getTaxRegistration().getId());
        assertNotNull(message.getBuyer());
        assertEquals("Buyer Company SAS", message.getBuyer().getName());
        assertEquals("Paris", message.getBuyer().getAddress().getCity());
    }
}
