package com.autoever.mes.mapper;

import com.autoever.mes.domain.history.entity.ProductionHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HistoryMapper {
    
    List<ProductionHistory> findHierarchyByOrderId(@Param("orderId") Long orderId);
}
