package com.autoever.mes.domain.quality.controller;

import com.autoever.mes.domain.quality.service.QualityInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/quality")
@RequiredArgsConstructor
public class QualityWebController {
    
    private final QualityInspectionService service;
    
    @GetMapping
    public String qualityPage(Model model) {
        model.addAttribute("inspections", service.findAll());
        return "quality/list";
    }
}
