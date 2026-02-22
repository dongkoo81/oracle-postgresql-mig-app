package com.autoever.mes.domain.spec.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "PRODUCT_SPEC")
@Getter @Setter
@NoArgsConstructor
public class ProductSpec {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "spec_seq")
    @SequenceGenerator(name = "spec_seq", sequenceName = "PRODUCT_SPEC_SEQ", allocationSize = 1)
    @Column(name = "SPEC_ID")
    private Long specId;
    
    @Column(name = "PRODUCT_ID", unique = true, nullable = false)
    private Long productId;
    
    @Lob
    @Column(name = "SPEC_XML", columnDefinition = "XMLTYPE")
    private String specXml;
    
    @Column(name = "VERSION", length = 20)
    private String version;
    
    @Column(name = "CREATED_DATE")
    private LocalDate createdDate;
}
