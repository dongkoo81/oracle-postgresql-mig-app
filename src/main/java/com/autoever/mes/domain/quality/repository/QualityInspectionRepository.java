package com.autoever.mes.domain.quality.repository;

import com.autoever.mes.domain.quality.entity.QualityInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QualityInspectionRepository extends JpaRepository<QualityInspection, QualityInspection.QualityInspectionId> {
    
    List<QualityInspection> findByResult(String result);
    
    @Query(value = "SELECT * FROM QUALITY_INSPECTION PARTITION(p_202412_sp_pass)", nativeQuery = true)
    List<QualityInspection> findFromPassPartition();
    
    @Query(value = "SELECT * FROM QUALITY_INSPECTION WHERE RESULT = :result ORDER BY INSPECTION_DATE DESC", nativeQuery = true)
    List<QualityInspection> findByResultNative(@Param("result") String result);
}
