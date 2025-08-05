package com.cii.messaging.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data
@Builder
public class PaymentTerms {
    private String description;
    private LocalDate dueDate;
    private String paymentMeansCode;
    private String accountNumber;
    private String accountName;
    private String financialInstitution;
}
