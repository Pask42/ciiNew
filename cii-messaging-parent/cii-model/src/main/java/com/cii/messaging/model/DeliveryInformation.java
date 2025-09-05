package com.cii.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class DeliveryInformation {
    private OffsetDateTime deliveryDate;
    private String deliveryLocationId;
    private Address deliveryAddress;
    private String deliveryPartyName;
}
