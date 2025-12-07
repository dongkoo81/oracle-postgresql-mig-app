package com.autoever.mes.domain.history.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDailySummary is a Querydsl query type for DailySummary
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDailySummary extends EntityPathBase<DailySummary> {

    private static final long serialVersionUID = 1192932699L;

    public static final QDailySummary dailySummary = new QDailySummary("dailySummary");

    public final DatePath<java.time.LocalDate> summaryDate = createDate("summaryDate", java.time.LocalDate.class);

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    public final NumberPath<Integer> totalOrders = createNumber("totalOrders", Integer.class);

    public QDailySummary(String variable) {
        super(DailySummary.class, forVariable(variable));
    }

    public QDailySummary(Path<? extends DailySummary> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDailySummary(PathMetadata metadata) {
        super(DailySummary.class, metadata);
    }

}

