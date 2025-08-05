package com.cii.messaging.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data
@Builder
public class DeliveryInformation {
    private LocalDate deliveryDate;
    private String deliveryLocationId;
    private Address deliveryAddress;
    private String deliveryPartyName;
}
