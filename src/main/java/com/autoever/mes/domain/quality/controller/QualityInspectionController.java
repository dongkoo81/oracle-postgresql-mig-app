package com.autoever.mes.domain.quality.controller;

import com.autoever.mes.domain.quality.entity.QualityInspection;
import com.autoever.mes.domain.quality.service.QualityInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class QualityInspectionController {
    
    private final QualityInspectionService service;
    
    @GetMapping
    public List<QualityInspection> getAll() {
        return service.findAll();
    }
    
    @GetMapping("/result/{result}")
    public List<QualityInspection> getByResult(@PathVariable String result) {
        return service.findByResult(result);
    }
    
    @PostMapping
    public QualityInspection create(@RequestBody QualityInspection inspection) {
        return service.save(inspection);
    }
}
