package com.autoever.mes.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    
    void calculateOrderTotal(@Param("orderId") Long orderId, @Param("result") Map<String, Object> result);
    
    Integer checkProductAvailable(@Param("productId") Long productId, @Param("requiredQty") Long requiredQty);
    
    String getProductStatus(@Param("productId") Long productId);
    
    void mergeInventory(@Param("productId") Long productId, @Param("quantity") Long quantity);
    
    List<Map<String, Object>> findOrdersByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
