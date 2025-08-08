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
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderResponseWriterTest {

    private CIIMessage sampleMessage() {
        LineItem line = LineItem.builder()
                .lineNumber("1")
                .productId("4012345678901")
                .description("Item A")
                .quantity(BigDecimal.ONE)
                .unitCode("EA")
                .build();

        DocumentHeader header = DocumentHeader.builder()
                .documentNumber("ORD-RSP-2024-001")
                .build();

        return CIIMessage.builder()
                .messageId("MSG-ORDERSP")
                .messageType(MessageType.ORDERSP)
                .creationDateTime(LocalDateTime.parse("20240201120000", DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                .seller(TradeParty.builder().id("SELLER").name("Seller Corp").build())
                .buyer(TradeParty.builder().id("BUYER").name("Buyer Corp").build())
                .header(header)
                .lineItems(java.util.List.of(line))
                .build();
    }

    @Test
    void generatedXmlShouldContainDataAndValidate() throws Exception {
        CIIMessage message = sampleMessage();
        OrderResponseWriter writer = new OrderResponseWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(message, out);
        byte[] xml = out.toByteArray();
        String xmlString = new String(xml);

        assertTrue(xmlString.contains("ORD-RSP-2024-001"));
        assertTrue(xmlString.contains("4012345678901"));

        Path xsd = Path.of("..", "cii-validator", "src", "main", "resources", "xsd", "d16b", "CrossIndustryOrderResponse.xsd");
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(xsd.toFile());
        Validator validator = schema.newValidator();

        assertDoesNotThrow(() -> {
            try {
                validator.validate(new StreamSource(new ByteArrayInputStream(xml)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
