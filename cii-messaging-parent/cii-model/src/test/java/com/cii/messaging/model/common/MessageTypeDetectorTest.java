package com.cii.messaging.model.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTypeDetectorTest {

    @Test
    void detectsInvoiceMessage() throws Exception {
        String xml = """
                <rsm:CrossIndustryInvoice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100\">
                </rsm:CrossIndustryInvoice>
                """;

        MessageType type = MessageTypeDetector.detect(xml);
        assertEquals(MessageType.INVOICE, type);
    }

    @Test
    void rejectsDoctypeUsage() {
        String xml = """
                <!DOCTYPE foo [<!ELEMENT foo ANY>]>
                <foo />
                """;

        MessageTypeDetectionException ex = assertThrows(MessageTypeDetectionException.class,
                () -> MessageTypeDetector.detect(xml));
        assertEquals(MessageTypeDetectionException.Reason.PROHIBITED_DTD, ex.getReason());
    }

    @Test
    void reportsUnknownRootElement() {
        String xml = """
                <UnknownRoot xmlns=\"urn:test\" />
                """;

        MessageTypeDetectionException ex = assertThrows(MessageTypeDetectionException.class,
                () -> MessageTypeDetector.detect(xml));
        assertEquals(MessageTypeDetectionException.Reason.UNKNOWN_ROOT, ex.getReason());
        assertTrue(ex.getMessage().contains("UnknownRoot"));
    }

    @Test
    void failsOnEmptyDocument() {
        MessageTypeDetectionException ex = assertThrows(MessageTypeDetectionException.class,
                () -> MessageTypeDetector.detect("   \n\t"));
        assertEquals(MessageTypeDetectionException.Reason.EMPTY_DOCUMENT, ex.getReason());
    }
}
