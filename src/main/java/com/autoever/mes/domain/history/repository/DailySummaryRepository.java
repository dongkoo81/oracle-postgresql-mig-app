package com.autoever.mes.domain.history.repository;

import com.autoever.mes.domain.history.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface DailySummaryRepository extends JpaRepository<DailySummary, LocalDateTime> {
    List<DailySummary> findBySummaryDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW DAILY_SUMMARY", nativeQuery = true)
    void refreshMaterializedView();
}
