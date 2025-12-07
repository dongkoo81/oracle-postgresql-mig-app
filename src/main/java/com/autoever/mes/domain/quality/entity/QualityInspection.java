package com.autoever.mes.domain.quality.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

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
    @Column(name = "INSPECTION_DATE")
    private LocalDate inspectionDate;
    
    @Id
    @Column(name = "RESULT", length = 20)
    private String result;
    
    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;
    
    @Column(name = "ORDER_ID")
    private Long orderId;
    
    @Column(name = "DEFECT_COUNT")
    private Integer defectCount = 0;
    
    @Column(name = "INSPECTOR_NAME", length = 100)
    private String inspectorName;
    
    @Lob
    @Column(name = "NOTES")
    private String notes;
    
    @Getter @Setter
    @NoArgsConstructor
    public static class QualityInspectionId implements Serializable {
        private Long inspectionId;
        private LocalDate inspectionDate;
        private String result;
    }
}
