package com.kaii.dentix.domain.subscription.dto;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSubscriptionResponseDto_SubscriptionCycle is a Querydsl query type for SubscriptionCycle
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSubscriptionResponseDto_SubscriptionCycle extends EntityPathBase<SubscriptionResponseDto.SubscriptionCycle> {

    private static final long serialVersionUID = 1394287161L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSubscriptionResponseDto_SubscriptionCycle subscriptionCycle = new QSubscriptionResponseDto_SubscriptionCycle("subscriptionCycle");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final BooleanPath active = createBoolean("active");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final com.kaii.dentix.domain.organization.domain.QOrganization organization;

    public final DateTimePath<java.time.LocalDateTime> periodEnd = createDateTime("periodEnd", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> periodStart = createDateTime("periodStart", java.time.LocalDateTime.class);

    public final com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan subscriptionPlan;

    public final NumberPath<Integer> successCount = createNumber("successCount", Integer.class);

    public QSubscriptionResponseDto_SubscriptionCycle(String variable) {
        this(SubscriptionResponseDto.SubscriptionCycle.class, forVariable(variable), INITS);
    }

    public QSubscriptionResponseDto_SubscriptionCycle(Path<? extends SubscriptionResponseDto.SubscriptionCycle> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSubscriptionResponseDto_SubscriptionCycle(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSubscriptionResponseDto_SubscriptionCycle(PathMetadata metadata, PathInits inits) {
        this(SubscriptionResponseDto.SubscriptionCycle.class, metadata, inits);
    }

    public QSubscriptionResponseDto_SubscriptionCycle(Class<? extends SubscriptionResponseDto.SubscriptionCycle> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new com.kaii.dentix.domain.organization.domain.QOrganization(forProperty("organization"), inits.get("organization")) : null;
        this.subscriptionPlan = inits.isInitialized("subscriptionPlan") ? new com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan(forProperty("subscriptionPlan")) : null;
    }

}

