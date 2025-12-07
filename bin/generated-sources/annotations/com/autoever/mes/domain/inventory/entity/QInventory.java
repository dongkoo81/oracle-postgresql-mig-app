package com.autoever.mes.domain.inventory.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QInventory is a Querydsl query type for Inventory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInventory extends EntityPathBase<Inventory> {

    private static final long serialVersionUID = -1270050426L;

    public static final QInventory inventory = new QInventory("inventory");

    public final NumberPath<Long> inventoryId = createNumber("inventoryId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastUpdated = createDateTime("lastUpdated", java.time.LocalDateTime.class);

    public final NumberPath<Long> productId = createNumber("productId", Long.class);

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final NumberPath<Long> version = createNumber("version", Long.class);

    public QInventory(String variable) {
        super(Inventory.class, forVariable(variable));
    }

    public QInventory(Path<? extends Inventory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QInventory(PathMetadata metadata) {
        super(Inventory.class, metadata);
    }

}

