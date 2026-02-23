package com.autoever.mes.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "INVENTORY")
@Getter @Setter
@NoArgsConstructor
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_seq")
    @SequenceGenerator(name = "inventory_seq", sequenceName = "INVENTORY_SEQ", allocationSize = 1)
    @Column(name = "INVENTORY_ID")
    private Long inventoryId;
    
    @Column(name = "PRODUCT_ID", unique = true, nullable = false)
    private Long productId;
    
    @Column(name = "QUANTITY", nullable = false)
    private Long quantity;
    
    @Column(name = "LAST_UPDATED", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastUpdated;
    
    @Version
    @Column(name = "VERSION", columnDefinition = "NUMERIC")
    private Long version;
}
