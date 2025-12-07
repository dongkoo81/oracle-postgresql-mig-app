package com.autoever.mes.domain.product.service;

import com.autoever.mes.common.dto.CreateProductRequest;
import com.autoever.mes.common.dto.ProductDto;
import com.autoever.mes.domain.product.entity.Product;
import com.autoever.mes.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public Page<ProductDto> searchProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::convertToDto);
    }
    
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<ProductDto> getActiveProducts() {
        return getAllProducts();
    }
    
    public ProductDto getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
    
    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setUnitPrice(request.getUnitPrice());
        product.setCreatedDate(LocalDate.now());
        
        Product saved = productRepository.save(product);
        return convertToDto(saved);
    }
    
    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setProductId(product.getProductId());
        dto.setProductCode(product.getProductCode());
        dto.setProductName(product.getProductName());
        dto.setUnitPrice(product.getUnitPrice());
        dto.setCreatedDate(product.getCreatedDate());
        return dto;
    }
}
