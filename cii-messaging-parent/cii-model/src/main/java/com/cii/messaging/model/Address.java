package com.cii.messaging.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class Address {
    private String street;
    private String city;
    private String postalCode;
    private String countryCode;
    private String countryName;
}
