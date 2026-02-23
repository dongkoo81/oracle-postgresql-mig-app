package com.autoever.mes.domain.history.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DAILY_SUMMARY")
@Immutable
@Getter @Setter
@NoArgsConstructor
public class DailySummary {
    
    @Id
    @Column(name = "SUMMARY_DATE")
    private LocalDateTime summaryDate;
    
    @Column(name = "TOTAL_ORDERS")
    private Long totalOrders;
    
    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2)
    private BigDecimal totalAmount;
}
