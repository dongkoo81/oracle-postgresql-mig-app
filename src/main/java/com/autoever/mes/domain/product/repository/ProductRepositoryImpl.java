package com.autoever.mes.domain.product.repository;

import com.autoever.mes.domain.product.entity.Product;
import com.autoever.mes.domain.product.entity.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    
    @Override
    public List<Product> searchProducts(String productName, BigDecimal minPrice, BigDecimal maxPrice) {
        QProduct product = QProduct.product;
        BooleanBuilder builder = new BooleanBuilder();
        
        if (productName != null && !productName.isEmpty()) {
            builder.and(product.productName.containsIgnoreCase(productName));
        }
        
        if (minPrice != null) {
            builder.and(product.unitPrice.goe(minPrice));
        }
        
        if (maxPrice != null) {
            builder.and(product.unitPrice.loe(maxPrice));
        }
        
        return queryFactory
            .selectFrom(product)
            .where(builder)
            .orderBy(product.productCode.asc())
            .fetch();
    }
    
    @Override
    public List<Product> findProductsCreatedToday() {
        String sql = "SELECT * FROM PRODUCT WHERE TRUNC(CREATED_DATE) = TRUNC(SYSDATE)";
        Query query = entityManager.createNativeQuery(sql, Product.class);
        return query.getResultList();
    }
    
    @Override
    public List<Product> findTopProductsByRownum(Integer limit) {
        String sql = "SELECT * FROM (SELECT * FROM PRODUCT ORDER BY UNIT_PRICE DESC) WHERE ROWNUM <= :limit";
        Query query = entityManager.createNativeQuery(sql, Product.class);
        query.setParameter("limit", limit);
        return query.getResultList();
    }
    
    @Override
    public Long getSequenceNextVal(String sequenceName) {
        String sql = "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
        Query query = entityManager.createNativeQuery(sql);
        return ((Number) query.getSingleResult()).longValue();
    }
    
    @Override
    public List<Product> findProductsWithoutInventory() {
        String sql = "SELECT * FROM PRODUCT MINUS SELECT p.* FROM PRODUCT p, INVENTORY i WHERE p.PRODUCT_ID = i.PRODUCT_ID";
        Query query = entityManager.createNativeQuery(sql, Product.class);
        return query.getResultList();
    }
    
    @Override
    public List<Map<String, Object>> findProductsWithInventoryOldStyle() {
        String sql = "SELECT p.PRODUCT_ID, p.PRODUCT_CODE, p.PRODUCT_NAME, p.UNIT_PRICE, i.QUANTITY " +
                     "FROM PRODUCT p, INVENTORY i " +
                     "WHERE p.PRODUCT_ID = i.PRODUCT_ID(+) " +
                     "ORDER BY p.PRODUCT_ID";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        
        return results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", row[0]);
            map.put("productCode", row[1]);
            map.put("productName", row[2]);
            map.put("unitPrice", row[3]);
            map.put("quantity", row[4]);
            return map;
        }).collect(Collectors.toList());
    }
}
