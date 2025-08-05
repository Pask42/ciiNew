package com.cii.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class DocumentHeader {
    private String documentNumber;
    private LocalDate documentDate;
    private String buyerReference;
    private String sellerReference;
    private String contractReference;
    private String currency;
    private PaymentTerms paymentTerms;
    private DeliveryInformation delivery;
}
