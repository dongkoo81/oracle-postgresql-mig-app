package com.autoever.mes.domain.history.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProductionHistory is a Querydsl query type for ProductionHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductionHistory extends EntityPathBase<ProductionHistory> {

    private static final long serialVersionUID = 1743216717L;

    public static final QProductionHistory productionHistory = new QProductionHistory("productionHistory");

    public final NumberPath<Long> historyId = createNumber("historyId", Long.class);

    public final NumberPath<Long> orderId = createNumber("orderId", Long.class);

    public final NumberPath<Long> parentId = createNumber("parentId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> processDate = createDateTime("processDate", java.time.LocalDateTime.class);

    public final StringPath processName = createString("processName");

    public QProductionHistory(String variable) {
        super(ProductionHistory.class, forVariable(variable));
    }

    public QProductionHistory(Path<? extends ProductionHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductionHistory(PathMetadata metadata) {
        super(ProductionHistory.class, metadata);
    }

}

