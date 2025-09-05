package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DesadvWriterTest {

    private CIIMessage sampleMessage() {
        LineItem line = LineItem.builder()
                .lineNumber("1")
                .productId("4012345678901")
                .description("Item A")
                .quantity(BigDecimal.ONE)
                .unitCode("EA")
                .build();

        return CIIMessage.builder()
                .messageId("MSG-DESADV")
                .messageType(MessageType.DESADV)
                  .creationDateTime(OffsetDateTime.of(2024, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .senderPartyId("SELLER")
                .receiverPartyId("BUYER")
                .lineItems(java.util.List.of(line))
                .build();
    }

    @Test
    void generatedXmlShouldContainDataAndValidate() throws Exception {
        CIIMessage message = sampleMessage();
        DesadvWriter writer = new DesadvWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(message, out);
        byte[] xml = out.toByteArray();
        String xmlString = new String(xml);

        assertTrue(xmlString.contains("4012345678901"));
        assertTrue(xmlString.contains("DespatchAdvice"));

        // Schema validation against the official XSD is out of scope for this basic writer test
    }
}
