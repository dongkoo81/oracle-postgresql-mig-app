package com.autoever.mes.domain.history.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCTION_HISTORY")
@Getter @Setter
@NoArgsConstructor
public class ProductionHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "history_seq")
    @SequenceGenerator(name = "history_seq", sequenceName = "PRODUCTION_HISTORY_SEQ", allocationSize = 1)
    @Column(name = "HISTORY_ID")
    private Long historyId;
    
    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;
    
    @Column(name = "PARENT_ID")
    private Long parentId;
    
    @Column(name = "PROCESS_NAME", length = 100)
    private String processName;
    
    @Column(name = "PROCESS_DATE")
    private LocalDateTime processDate;
}
