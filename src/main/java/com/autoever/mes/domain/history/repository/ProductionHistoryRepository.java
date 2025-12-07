package com.autoever.mes.domain.history.repository;

import com.autoever.mes.domain.history.entity.ProductionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionHistoryRepository extends JpaRepository<ProductionHistory, Long> {
    List<ProductionHistory> findByOrderId(Long orderId);
}
