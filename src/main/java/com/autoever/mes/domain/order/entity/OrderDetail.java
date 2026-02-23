package com.autoever.mes.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ORDER_DETAIL")
@Getter @Setter
@NoArgsConstructor
public class OrderDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_detail_seq")
    @SequenceGenerator(name = "order_detail_seq", sequenceName = "ORDER_DETAIL_SEQ", allocationSize = 1)
    @Column(name = "DETAIL_ID")
    private Long detailId;
    
    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;
    
    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;
    
    @Column(name = "QUANTITY", nullable = false)
    private Long quantity;
    
    @Column(name = "UNIT_PRICE", precision = 15, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "LINE_AMOUNT", precision = 15, scale = 2)
    private BigDecimal lineAmount;
}
