package com.cii.messaging.model;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
public class TotalsInformation {
    private BigDecimal lineTotalAmount;
    private BigDecimal taxBasisAmount;
    private BigDecimal taxTotalAmount;
    private BigDecimal grandTotalAmount;
    private BigDecimal prepaidAmount;
    private BigDecimal duePayableAmount;
}
