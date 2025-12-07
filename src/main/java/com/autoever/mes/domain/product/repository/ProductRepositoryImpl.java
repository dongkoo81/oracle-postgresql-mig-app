package com.autoever.mes.domain.product.repository;

import com.autoever.mes.domain.product.entity.Product;
import com.autoever.mes.domain.product.entity.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
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
}
