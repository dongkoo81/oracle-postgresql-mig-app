package com.autoever.mes.domain.spec.repository;

import com.autoever.mes.domain.spec.entity.ProductSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {
    Optional<ProductSpec> findByProductId(Long productId);
    List<ProductSpec> findAllByProductId(Long productId);
}
