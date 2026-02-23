package com.autoever.mes.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDto {
    private Long productId;
    private String productCode;
    private String productName;
    private BigDecimal unitPrice;
    private LocalDateTime createdDate;
}
