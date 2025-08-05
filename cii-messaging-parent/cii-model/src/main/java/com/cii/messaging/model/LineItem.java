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
public class LineItem {
    private String lineNumber;
    private String productId;
    private String description;
    private BigDecimal quantity;
    private String unitCode;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private BigDecimal taxRate;
    private String taxCategory;
    private String taxTypeCode;
}
