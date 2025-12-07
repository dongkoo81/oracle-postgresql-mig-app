package com.autoever.mes.domain.spec.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProductSpec is a Querydsl query type for ProductSpec
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductSpec extends EntityPathBase<ProductSpec> {

    private static final long serialVersionUID = -162321073L;

    public static final QProductSpec productSpec = new QProductSpec("productSpec");

    public final DatePath<java.time.LocalDate> createdDate = createDate("createdDate", java.time.LocalDate.class);

    public final NumberPath<Long> productId = createNumber("productId", Long.class);

    public final NumberPath<Long> specId = createNumber("specId", Long.class);

    public final StringPath specXml = createString("specXml");

    public final StringPath version = createString("version");

    public QProductSpec(String variable) {
        super(ProductSpec.class, forVariable(variable));
    }

    public QProductSpec(Path<? extends ProductSpec> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductSpec(PathMetadata metadata) {
        super(ProductSpec.class, metadata);
    }

}

