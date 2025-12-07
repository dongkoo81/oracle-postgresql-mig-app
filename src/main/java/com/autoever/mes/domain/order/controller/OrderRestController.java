package com.autoever.mes.domain.order.controller;

import com.autoever.mes.domain.order.entity.ProductionOrder;
import com.autoever.mes.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderRestController {
    
    private final OrderService orderService;
    
    @GetMapping
    public ResponseEntity<List<ProductionOrder>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductionOrder> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
    
    @PostMapping
    public ResponseEntity<ProductionOrder> createOrder(@RequestBody CreateOrderRequest request) {
        ProductionOrder order = orderService.createOrder(
            request.getOrderNo(),
            request.getNotes(),
            request.getDetails()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    public static class CreateOrderRequest {
        private String orderNo;
        private String notes;
        private List<OrderService.OrderDetailRequest> details;
        
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<OrderService.OrderDetailRequest> getDetails() { return details; }
        public void setDetails(List<OrderService.OrderDetailRequest> details) { this.details = details; }
    }
}
