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
                .creationDateTime(LocalDateTime.parse("20240201120000", DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
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

        Path xsd = Path.of("..", "cii-validator", "src", "main", "resources", "xsd", "d16b", "CrossIndustryDespatchAdvice.xsd");
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
