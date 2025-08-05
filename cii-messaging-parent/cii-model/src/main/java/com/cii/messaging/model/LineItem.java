package com.cii.messaging.model;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
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
}
