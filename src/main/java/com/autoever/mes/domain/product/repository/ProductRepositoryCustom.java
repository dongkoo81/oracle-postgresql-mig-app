package com.autoever.mes.domain.product.repository;

import com.autoever.mes.domain.product.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductRepositoryCustom {
    List<Product> searchProducts(String productName, BigDecimal minPrice, BigDecimal maxPrice);
    
    // SYSDATE 테스트
    List<Product> findProductsCreatedToday();
    
    // ROWNUM 테스트
    List<Product> findTopProductsByRownum(Integer limit);
    
    // Sequence NEXTVAL 테스트
    Long getSequenceNextVal(String sequenceName);
    
    // MINUS 테스트
    List<Product> findProductsWithoutInventory();
    
    // (+) Outer Join 테스트
    List<Map<String, Object>> findProductsWithInventoryOldStyle();
}
