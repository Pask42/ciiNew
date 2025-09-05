package com.cii.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class CIIMessage {
    private String messageId;
    private MessageType messageType;
    private OffsetDateTime creationDateTime;
    private String senderPartyId;
    private String receiverPartyId;
    private TradeParty seller;
    private TradeParty buyer;
    private DocumentHeader header;
    private List<LineItem> lineItems;
    private TotalsInformation totals;
}
