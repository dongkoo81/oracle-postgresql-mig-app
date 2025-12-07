package com.autoever.mes.domain.product.controller;

import com.autoever.mes.common.dto.CreateProductRequest;
import com.autoever.mes.common.dto.ProductDto;
import com.autoever.mes.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRestController {
    
    private final ProductService productService;
    
    @GetMapping
    public Page<ProductDto> getProducts(Pageable pageable) {
        return productService.searchProducts(pageable);
    }
    
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDto created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
