package com.cii.messaging.service.impl;

import com.cii.messaging.model.CIIMessage;
import com.cii.messaging.model.MessageType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CIIMessagingServiceImplTest {

    @Test
    void testJsonConversionRoundTrip() throws Exception {
        CIIMessagingServiceImpl service = new CIIMessagingServiceImpl();

        CIIMessage original = CIIMessage.builder()
                .messageId("123")
                .messageType(MessageType.ORDER)
                .creationDateTime(LocalDateTime.now())
                .build();

        String json = service.convertToJson(original);
        CIIMessage result = service.convertFromJson(json, MessageType.ORDER);

        assertEquals(original.getMessageId(), result.getMessageId());
        assertEquals(original.getMessageType(), result.getMessageType());
    }
}
