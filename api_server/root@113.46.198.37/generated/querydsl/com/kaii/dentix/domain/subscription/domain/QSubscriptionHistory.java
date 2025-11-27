package com.kaii.dentix.domain.subscription.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSubscriptionHistory is a Querydsl query type for SubscriptionHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSubscriptionHistory extends EntityPathBase<SubscriptionHistory> {

    private static final long serialVersionUID = -2025570879L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSubscriptionHistory subscriptionHistory = new QSubscriptionHistory("subscriptionHistory");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final DateTimePath<java.time.LocalDateTime> endDate = createDateTime("endDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final com.kaii.dentix.domain.organization.domain.QOrganization organization;

    public final StringPath reason = createString("reason");

    public final DateTimePath<java.time.LocalDateTime> startDate = createDateTime("startDate", java.time.LocalDateTime.class);

    public final QSubscriptionPlan subscriptionPlan;

    public QSubscriptionHistory(String variable) {
        this(SubscriptionHistory.class, forVariable(variable), INITS);
    }

    public QSubscriptionHistory(Path<? extends SubscriptionHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSubscriptionHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSubscriptionHistory(PathMetadata metadata, PathInits inits) {
        this(SubscriptionHistory.class, metadata, inits);
    }

    public QSubscriptionHistory(Class<? extends SubscriptionHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new com.kaii.dentix.domain.organization.domain.QOrganization(forProperty("organization"), inits.get("organization")) : null;
        this.subscriptionPlan = inits.isInitialized("subscriptionPlan") ? new QSubscriptionPlan(forProperty("subscriptionPlan")) : null;
    }

}

