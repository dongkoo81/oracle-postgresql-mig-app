package com.autoever.mes.domain.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT")
@Getter @Setter
@NoArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "PRODUCT_SEQ", allocationSize = 1)
    @Column(name = "PRODUCT_ID")
    private Long productId;
    
    @Column(name = "PRODUCT_CODE", unique = true, nullable = false, length = 50)
    private String productCode;
    
    @Column(name = "PRODUCT_NAME", nullable = false, length = 200)
    private String productName;
    
    @Column(name = "UNIT_PRICE", precision = 15, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "CREATED_DATE", columnDefinition = "TIMESTAMP")
    private LocalDateTime createdDate;
}
