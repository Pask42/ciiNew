package com.cii.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class TradeParty {
    private String id;
    private String name;
    private Address address;
    private Contact contact;
    private TaxRegistration taxRegistration;
}
