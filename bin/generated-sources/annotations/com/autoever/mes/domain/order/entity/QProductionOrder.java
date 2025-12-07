package com.autoever.mes.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProductionOrder is a Querydsl query type for ProductionOrder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductionOrder extends EntityPathBase<ProductionOrder> {

    private static final long serialVersionUID = 427154957L;

    public static final QProductionOrder productionOrder = new QProductionOrder("productionOrder");

    public final StringPath notes = createString("notes");

    public final DatePath<java.time.LocalDate> orderDate = createDate("orderDate", java.time.LocalDate.class);

    public final NumberPath<Long> orderId = createNumber("orderId", Long.class);

    public final StringPath orderNo = createString("orderNo");

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    public QProductionOrder(String variable) {
        super(ProductionOrder.class, forVariable(variable));
    }

    public QProductionOrder(Path<? extends ProductionOrder> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductionOrder(PathMetadata metadata) {
        super(ProductionOrder.class, metadata);
    }

}

