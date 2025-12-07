package com.autoever.mes.domain.document.repository;

import com.autoever.mes.domain.document.entity.ProductDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductDocumentRepository extends JpaRepository<ProductDocument, Long> {
    List<ProductDocument> findByProductId(Long productId);
}
