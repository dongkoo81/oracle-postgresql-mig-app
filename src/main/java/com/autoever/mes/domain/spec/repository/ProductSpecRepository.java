package com.autoever.mes.domain.spec.repository;

import com.autoever.mes.domain.spec.entity.ProductSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {
    Optional<ProductSpec> findByProductId(Long productId);
    List<ProductSpec> findAllByProductId(Long productId);
    
    @Modifying
    @Query(value = "UPDATE product_spec SET spec_xml = XMLPARSE(DOCUMENT :xmlContent), version = :version WHERE spec_id = :specId", nativeQuery = true)
    void updateSpecXml(@Param("specId") Long specId, @Param("xmlContent") String xmlContent, @Param("version") String version);
    
    @Modifying
    @Query(value = "INSERT INTO product_spec (product_id, spec_xml, version) VALUES (:productId, XMLPARSE(DOCUMENT :xmlContent), :version)", nativeQuery = true)
    void insertSpecXml(@Param("productId") Long productId, @Param("xmlContent") String xmlContent, @Param("version") String version);
}
