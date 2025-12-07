package com.autoever.mes.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    
    @NotBlank(message = "제품 코드는 필수입니다")
    private String productCode;
    
    @NotBlank(message = "제품명은 필수입니다")
    private String productName;
    
    @NotNull(message = "단가는 필수입니다")
    @Positive(message = "단가는 양수여야 합니다")
    private BigDecimal unitPrice;
}
