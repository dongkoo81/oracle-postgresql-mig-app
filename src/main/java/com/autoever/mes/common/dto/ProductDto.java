package com.autoever.mes.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductDto {
    private Long productId;
    private String productCode;
    private String productName;
    private BigDecimal unitPrice;
    private LocalDate createdDate;
}
