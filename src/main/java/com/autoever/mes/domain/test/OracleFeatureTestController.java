package com.autoever.mes.domain.test;

import com.autoever.mes.domain.document.entity.ProductDocument;
import com.autoever.mes.domain.document.service.DocumentService;
import com.autoever.mes.domain.history.entity.DailySummary;
import com.autoever.mes.domain.history.entity.ProductionHistory;
import com.autoever.mes.domain.history.repository.DailySummaryRepository;
import com.autoever.mes.domain.product.entity.Product;
import com.autoever.mes.domain.product.repository.ProductRepository;
import com.autoever.mes.domain.quality.entity.QualityInspection;
import com.autoever.mes.domain.quality.service.QualityInspectionService;
import com.autoever.mes.domain.spec.entity.ProductSpec;
import com.autoever.mes.domain.spec.service.SpecService;
import com.autoever.mes.mapper.HistoryMapper;
import com.autoever.mes.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/oracle")
@RequiredArgsConstructor
public class OracleFeatureTestController {
    
    private final OrderMapper orderMapper;
    private final HistoryMapper historyMapper;
    private final ProductRepository productRepository;
    private final DocumentService documentService;
    private final SpecService specService;
    private final DailySummaryRepository dailySummaryRepository;
    private final QualityInspectionService qualityInspectionService;
    
    // 1. Stored Procedure 테스트
    @PostMapping("/procedure/calculate-total/{orderId}")
    public Map<String, Object> testStoredProcedure(@PathVariable Long orderId) {
        Map<String, Object> result = new HashMap<>();
        orderMapper.calculateOrderTotal(orderId, result);
        return result;
    }
    
    // 2. Stored Function 테스트
    @GetMapping("/function/check-available")
    public Map<String, Object> testStoredFunction(
            @RequestParam Long productId,
            @RequestParam Long requiredQty) {
        Integer available = orderMapper.checkProductAvailable(productId, requiredQty);
        return Map.of("available", available != null && available == 1);
    }
    
    // 3. CONNECT BY 계층 쿼리 테스트
    @GetMapping("/hierarchy/{orderId}")
    public List<ProductionHistory> testConnectBy(@PathVariable Long orderId) {
        return historyMapper.findHierarchyByOrderId(orderId);
    }
    
    // 4. QueryDSL 동적 쿼리 테스트
    @GetMapping("/querydsl/search")
    public List<Product> testQueryDsl(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return productRepository.searchProducts(name, minPrice, maxPrice);
    }
    
    // 5. CLOB 테스트
    @PostMapping("/clob/save")
    public Map<String, Object> testClob(
            @RequestParam Long productId,
            @RequestParam String content) {
        var doc = documentService.saveDocument(productId, "Test Doc", content, null);
        return Map.of("docId", doc.getDocId(), "contentLength", content.length());
    }
    
    // 6. XML 테스트
    @PostMapping("/xml/save")
    public Map<String, Object> testXml(
            @RequestParam Long productId,
            @RequestParam String xmlContent) {
        var spec = specService.saveSpec(productId, xmlContent, "1.0");
        return Map.of("specId", spec.getSpecId());
    }
    
    // 7. Materialized View 조회 테스트
    @GetMapping("/materialized-view")
    public List<DailySummary> testMaterializedView() {
        return dailySummaryRepository.findAll();
    }
    
    // 8. Materialized View Refresh
    @PostMapping("/materialized-view/refresh")
    @Transactional
    public Map<String, Object> refreshMaterializedView() {
        dailySummaryRepository.refreshMaterializedView();
        return Map.of("message", "Materialized View refreshed successfully");
    }
    
    // 9. 문서 조회
    @GetMapping("/documents/product/{productId}")
    public List<ProductDocument> getDocumentsByProduct(@PathVariable Long productId) {
        return documentService.findByProductId(productId);
    }
    
    // 10. 사양 조회
    @GetMapping("/specs/product/{productId}")
    public List<ProductSpec> getSpecsByProduct(@PathVariable Long productId) {
        return specService.findAllByProductId(productId);
    }
    
    // 11. Partition Table 조회 테스트
    @GetMapping("/partition/{result}")
    public List<QualityInspection> testPartitionTable(@PathVariable String result) {
        return qualityInspectionService.findByResult(result);
    }
    
    // 12. DECODE 함수 테스트 (제품 상태 확인)
    @GetMapping("/decode/product-status/{productId}")
    public Map<String, Object> testDecodeFunction(@PathVariable Long productId) {
        String status = orderMapper.getProductStatus(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("status", status);
        return result;
    }
    
    // 13. MERGE 문 테스트 (재고 업데이트)
    @PostMapping("/merge/inventory")
    public Map<String, Object> testMergeStatement(
            @RequestParam Long productId,
            @RequestParam Long quantity) {
        orderMapper.mergeInventory(productId, quantity);
        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("quantityAdded", quantity);
        result.put("message", "Inventory merged successfully");
        return result;
    }
    
    // 14. SYSDATE 테스트 - 오늘 생성된 제품 조회
    @GetMapping("/sysdate/today-products")
    public List<Product> testSysdate() {
        return productRepository.findProductsCreatedToday();
    }
    
    // 15. TO_DATE 테스트 - 날짜 범위 검색
    @GetMapping("/to-date/search")
    public List<Map<String, Object>> testToDate(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return orderMapper.findOrdersByDateRange(startDate, endDate);
    }
    
    // 16. ROWNUM 테스트 - 직접 페이징
    @GetMapping("/rownum/top-products")
    public List<Product> testRownum(@RequestParam(defaultValue = "5") Integer limit) {
        return productRepository.findTopProductsByRownum(limit);
    }
    
    // 17. Sequence NEXTVAL 테스트 - 직접 호출
    @GetMapping("/sequence/nextval")
    public Map<String, Object> testSequenceNextval(@RequestParam String sequenceName) {
        Long nextVal = productRepository.getSequenceNextVal(sequenceName);
        return Map.of("sequenceName", sequenceName, "nextVal", nextVal);
    }
    
    // 18. MINUS 테스트 - 집합 연산
    @GetMapping("/minus/products-without-inventory")
    public List<Product> testMinus() {
        return productRepository.findProductsWithoutInventory();
    }
    
    // 19. (+) Outer Join 테스트 - 구식 문법
    @GetMapping("/outer-join/products-inventory")
    public List<Map<String, Object>> testOuterJoin() {
        return productRepository.findProductsWithInventoryOldStyle();
    }
}
