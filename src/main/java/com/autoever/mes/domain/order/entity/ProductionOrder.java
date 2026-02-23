package com.autoever.mes.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCTION_ORDER")
@Getter @Setter
@NoArgsConstructor
public class ProductionOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(name = "order_seq", sequenceName = "PRODUCTION_ORDER_SEQ", allocationSize = 1)
    @Column(name = "ORDER_ID")
    private Long orderId;
    
    @Column(name = "ORDER_NO", unique = true, nullable = false, length = 50)
    private String orderNo;
    
    @Column(name = "ORDER_DATE", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime orderDate;
    
    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "NOTES", columnDefinition = "TEXT")
    private String notes;
}
