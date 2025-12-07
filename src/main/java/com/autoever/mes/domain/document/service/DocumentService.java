package com.autoever.mes.domain.document.service;

import com.autoever.mes.domain.document.entity.ProductDocument;
import com.autoever.mes.domain.document.repository.ProductDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final ProductDocumentRepository documentRepository;
    
    @Transactional
    public ProductDocument saveDocument(Long productId, String docName, String content, byte[] fileData) {
        ProductDocument doc = new ProductDocument();
        doc.setProductId(productId);
        doc.setDocName(docName);
        doc.setDocContent(content);  // CLOB
        doc.setDocFile(fileData);    // BLOB
        return documentRepository.save(doc);
    }
    
    @Transactional(readOnly = true)
    public String readClobContent(Long docId) throws SQLException {
        ProductDocument doc = documentRepository.findById(docId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        return doc.getDocContent();
    }
    
    @Transactional(readOnly = true)
    public List<ProductDocument> findByProductId(Long productId) {
        return documentRepository.findByProductId(productId);
    }
}
