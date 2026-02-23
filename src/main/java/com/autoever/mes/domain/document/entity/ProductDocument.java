package com.autoever.mes.domain.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT_DOCUMENT")
@Getter @Setter
@NoArgsConstructor
public class ProductDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "document_seq")
    @SequenceGenerator(name = "document_seq", sequenceName = "PRODUCT_DOCUMENT_SEQ", allocationSize = 1)
    @Column(name = "DOC_ID")
    private Long docId;
    
    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;
    
    @Column(name = "DOC_NAME", length = 200)
    private String docName;
    
    @Column(name = "DOC_CONTENT", columnDefinition = "TEXT")
    private String docContent;
    
    @Column(name = "DOC_FILE", columnDefinition = "BYTEA")
    private byte[] docFile;
    
    @Column(name = "EXTERNAL_FILE", length = 500)
    private String externalFile;
    
    @Column(name = "CREATED_DATE", columnDefinition = "TIMESTAMP")
    private LocalDateTime createdDate;
}
