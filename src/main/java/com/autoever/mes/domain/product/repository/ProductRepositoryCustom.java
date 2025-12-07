package com.autoever.mes.domain.product.repository;

import com.autoever.mes.domain.product.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepositoryCustom {
    List<Product> searchProducts(String productName, BigDecimal minPrice, BigDecimal maxPrice);
}
