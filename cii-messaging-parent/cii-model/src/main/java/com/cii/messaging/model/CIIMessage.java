package com.cii.messaging.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CIIMessage {
    private String messageId;
    private MessageType messageType;
    private LocalDateTime creationDateTime;
    private String senderPartyId;
    private String receiverPartyId;
    private DocumentHeader header;
    private List<LineItem> lineItems;
    private TotalsInformation totals;
}
