package com.cii.messaging.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class TotalsInformation {
    private BigDecimal lineTotalAmount;
    private BigDecimal taxBasisAmount;
    private BigDecimal taxTotalAmount;
    private BigDecimal grandTotalAmount;
    private BigDecimal prepaidAmount;
    private BigDecimal duePayableAmount;
}
