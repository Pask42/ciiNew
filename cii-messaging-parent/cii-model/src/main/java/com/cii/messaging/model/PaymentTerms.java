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
public class PaymentTerms {
    private String description;
    private LocalDate dueDate;
    private String paymentMeansCode;
    private String accountNumber;
    private String accountName;
    private String financialInstitution;
}
