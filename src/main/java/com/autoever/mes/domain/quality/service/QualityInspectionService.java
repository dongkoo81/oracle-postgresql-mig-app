package com.autoever.mes.domain.quality.service;

import com.autoever.mes.domain.quality.entity.QualityInspection;
import com.autoever.mes.domain.quality.repository.QualityInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualityInspectionService {
    
    private final QualityInspectionRepository repository;
    
    public List<QualityInspection> findByResult(String result) {
        return repository.findByResultNative(result);
    }
    
    public List<QualityInspection> findAll() {
        return repository.findAll();
    }
    
    @Transactional
    public QualityInspection save(QualityInspection inspection) {
        return repository.save(inspection);
    }
}
