package com.kaii.dentix.domain.subscription.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSubscriptionUsage is a Querydsl query type for SubscriptionUsage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSubscriptionUsage extends EntityPathBase<SubscriptionUsage> {

    private static final long serialVersionUID = -128369138L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSubscriptionUsage subscriptionUsage = new QSubscriptionUsage("subscriptionUsage");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final BooleanPath active = createBoolean("active");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final com.kaii.dentix.domain.organization.domain.QOrganization organization;

    public final NumberPath<Integer> overuseCount = createNumber("overuseCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> periodEnd = createDateTime("periodEnd", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> periodStart = createDateTime("periodStart", java.time.LocalDateTime.class);

    public final NumberPath<Integer> remainingCount = createNumber("remainingCount", Integer.class);

    public final QSubscriptionPlan subscriptionPlan;

    public final NumberPath<Long> subscriptionUsageId = createNumber("subscriptionUsageId", Long.class);

    public final NumberPath<Integer> successCount = createNumber("successCount", Integer.class);

    public QSubscriptionUsage(String variable) {
        this(SubscriptionUsage.class, forVariable(variable), INITS);
    }

    public QSubscriptionUsage(Path<? extends SubscriptionUsage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSubscriptionUsage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSubscriptionUsage(PathMetadata metadata, PathInits inits) {
        this(SubscriptionUsage.class, metadata, inits);
    }

    public QSubscriptionUsage(Class<? extends SubscriptionUsage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new com.kaii.dentix.domain.organization.domain.QOrganization(forProperty("organization"), inits.get("organization")) : null;
        this.subscriptionPlan = inits.isInitialized("subscriptionPlan") ? new QSubscriptionPlan(forProperty("subscriptionPlan")) : null;
    }

}

