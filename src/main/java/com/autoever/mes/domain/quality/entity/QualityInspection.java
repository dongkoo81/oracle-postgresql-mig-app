package com.autoever.mes.domain.quality.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "QUALITY_INSPECTION")
@IdClass(QualityInspection.QualityInspectionId.class)
@Getter @Setter
@NoArgsConstructor
public class QualityInspection {
    
    @Id
    @Column(name = "INSPECTION_ID")
    private Long inspectionId;
    
    @Id
    @Column(name = "INSPECTION_DATE", columnDefinition = "TIMESTAMP")
    private LocalDateTime inspectionDate;
    
    @Id
    @Column(name = "RESULT", length = 20)
    private String result;
    
    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;
    
    @Column(name = "ORDER_ID")
    private Long orderId;
    
    @Column(name = "DEFECT_COUNT")
    private Long defectCount = 0L;
    
    @Column(name = "INSPECTOR_NAME", length = 100)
    private String inspectorName;
    
    @Column(name = "NOTES", columnDefinition = "TEXT")
    private String notes;
    
    @Getter @Setter
    @NoArgsConstructor
    public static class QualityInspectionId implements Serializable {
        private Long inspectionId;
        private LocalDateTime inspectionDate;
        private String result;
    }
}
