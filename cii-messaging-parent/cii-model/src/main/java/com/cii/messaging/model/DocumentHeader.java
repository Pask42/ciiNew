package com.cii.messaging.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data
@Builder
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
