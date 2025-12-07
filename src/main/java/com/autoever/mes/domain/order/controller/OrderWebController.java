package com.autoever.mes.domain.order.controller;

import com.autoever.mes.domain.order.service.OrderService;
import com.autoever.mes.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderWebController {
    
    private final OrderService orderService;
    private final ProductService productService;
    
    @GetMapping
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "orders/list";
    }
    
    @GetMapping("/new")
    public String newOrderForm(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("order", new OrderForm());
        return "orders/form";
    }
    
    @PostMapping
    public String createOrder(@ModelAttribute OrderForm form) {
        List<OrderService.OrderDetailRequest> details = new ArrayList<>();
        
        for (int i = 0; i < form.getProductIds().size(); i++) {
            if (form.getQuantities().get(i) > 0) {
                OrderService.OrderDetailRequest detail = new OrderService.OrderDetailRequest();
                detail.setProductId(form.getProductIds().get(i));
                detail.setQuantity(form.getQuantities().get(i));
                details.add(detail);
            }
        }
        
        orderService.createOrder(form.getOrderNo(), form.getNotes(), details);
        return "redirect:/orders";
    }
    
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        return "orders/detail";
    }
    
    public static class OrderForm {
        private String orderNo;
        private String notes;
        private List<Long> productIds = new ArrayList<>();
        private List<Integer> quantities = new ArrayList<>();
        
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<Long> getProductIds() { return productIds; }
        public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
        public List<Integer> getQuantities() { return quantities; }
        public void setQuantities(List<Integer> quantities) { this.quantities = quantities; }
    }
}
