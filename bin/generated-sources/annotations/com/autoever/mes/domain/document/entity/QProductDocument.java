package com.autoever.mes.domain.document.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProductDocument is a Querydsl query type for ProductDocument
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductDocument extends EntityPathBase<ProductDocument> {

    private static final long serialVersionUID = 1659199695L;

    public static final QProductDocument productDocument = new QProductDocument("productDocument");

    public final DatePath<java.time.LocalDate> createdDate = createDate("createdDate", java.time.LocalDate.class);

    public final StringPath docContent = createString("docContent");

    public final ArrayPath<byte[], Byte> docFile = createArray("docFile", byte[].class);

    public final NumberPath<Long> docId = createNumber("docId", Long.class);

    public final StringPath docName = createString("docName");

    public final StringPath externalFile = createString("externalFile");

    public final NumberPath<Long> productId = createNumber("productId", Long.class);

    public QProductDocument(String variable) {
        super(ProductDocument.class, forVariable(variable));
    }

    public QProductDocument(Path<? extends ProductDocument> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductDocument(PathMetadata metadata) {
        super(ProductDocument.class, metadata);
    }

}

