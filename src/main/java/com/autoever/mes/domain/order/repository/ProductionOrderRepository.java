package com.autoever.mes.domain.order.repository;

import com.autoever.mes.domain.order.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    List<ProductionOrder> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);
}
