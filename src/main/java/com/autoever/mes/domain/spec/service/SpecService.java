package com.autoever.mes.domain.spec.service;

import com.autoever.mes.domain.spec.entity.ProductSpec;
import com.autoever.mes.domain.spec.repository.ProductSpecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecService {
    
    private final ProductSpecRepository specRepository;
    
    @Transactional
    public ProductSpec saveSpec(Long productId, String xmlContent, String version) {
        // 기존 사양이 있으면 업데이트, 없으면 새로 생성
        ProductSpec spec = specRepository.findByProductId(productId)
                .orElse(new ProductSpec());
        
        spec.setProductId(productId);
        spec.setSpecXml(xmlContent);  // CLOB (XMLType)
        spec.setVersion(version);
        return specRepository.save(spec);
    }
    
    @Transactional(readOnly = true)
    public String getSpecXml(Long productId) {
        return specRepository.findByProductId(productId)
            .map(ProductSpec::getSpecXml)
            .orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<ProductSpec> findAllByProductId(Long productId) {
        return specRepository.findAllByProductId(productId);
    }
}
