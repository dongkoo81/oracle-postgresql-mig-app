package com.autoever.mes.domain.product.controller;

import com.autoever.mes.common.dto.CreateProductRequest;
import com.autoever.mes.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductWebController {
    
    private final ProductService productService;
    
    @GetMapping
    public String listProducts(Model model, @PageableDefault(size = 20) Pageable pageable) {
        model.addAttribute("products", productService.searchProducts(pageable));
        return "products/list";
    }
    
    @GetMapping("/new")
    public String newProductForm(Model model) {
        model.addAttribute("product", new CreateProductRequest());
        return "products/form";
    }
    
    @PostMapping
    public String createProduct(@Valid @ModelAttribute("product") CreateProductRequest request,
                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "products/form";
        }
        productService.createProduct(request);
        return "redirect:/products";
    }
}
