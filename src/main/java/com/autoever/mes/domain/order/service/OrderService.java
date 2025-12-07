package com.autoever.mes.domain.order.service;

import com.autoever.mes.domain.inventory.repository.InventoryRepository;
import com.autoever.mes.domain.order.entity.OrderDetail;
import com.autoever.mes.domain.order.entity.ProductionOrder;
import com.autoever.mes.domain.order.repository.OrderDetailRepository;
import com.autoever.mes.domain.order.repository.ProductionOrderRepository;
import com.autoever.mes.domain.product.repository.ProductRepository;
import com.autoever.mes.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    
    private final ProductionOrderRepository orderRepository;
    private final OrderDetailRepository detailRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderMapper orderMapper;
    
    public List<ProductionOrder> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public ProductionOrder getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    @Transactional
    public ProductionOrder createOrder(String orderNo, String notes, List<OrderDetailRequest> details) {
        // 1. 주문 생성
        ProductionOrder order = new ProductionOrder();
        order.setOrderNo(orderNo);
        order.setOrderDate(LocalDate.now());
        order.setNotes(notes);
        order.setTotalAmount(BigDecimal.ZERO);
        
        ProductionOrder savedOrder = orderRepository.save(order);
        
        // 2. 주문 상세 생성
        for (OrderDetailRequest req : details) {
            var product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
            
            // 재고 체크 (Stored Function)
            Integer available = orderMapper.checkProductAvailable(req.getProductId(), req.getQuantity());
            if (available != 1) {
                throw new RuntimeException("Insufficient inventory for product: " + product.getProductName());
            }
            
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(savedOrder.getOrderId());
            detail.setProductId(req.getProductId());
            detail.setQuantity(req.getQuantity());
            detail.setUnitPrice(product.getUnitPrice());
            detail.setLineAmount(product.getUnitPrice().multiply(BigDecimal.valueOf(req.getQuantity())));
            
            detailRepository.save(detail);
            
            // 재고 차감
            inventoryRepository.findByProductId(req.getProductId()).ifPresent(inv -> {
                inv.setQuantity(inv.getQuantity() - req.getQuantity());
                inventoryRepository.save(inv);
            });
        }
        
        // 3. 총액 계산 (Stored Procedure)
        Map<String, Object> result = new HashMap<>();
        orderMapper.calculateOrderTotal(savedOrder.getOrderId(), result);
        
        return savedOrder;
    }
    
    public static class OrderDetailRequest {
        private Long productId;
        private Integer quantity;
        
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
