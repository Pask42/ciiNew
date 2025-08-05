package com.cii.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
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
