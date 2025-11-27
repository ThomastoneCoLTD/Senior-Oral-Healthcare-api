package com.kaii.dentix.domain.organization.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrganizationSubscription is a Querydsl query type for OrganizationSubscription
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrganizationSubscription extends EntityPathBase<OrganizationSubscription> {

    private static final long serialVersionUID = -771255492L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrganizationSubscription organizationSubscription = new QOrganizationSubscription("organizationSubscription");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final BooleanPath autoRenew = createBoolean("autoRenew");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final QOrganization organization;

    public final NumberPath<Integer> remainingResponses = createNumber("remainingResponses", Integer.class);

    public final EnumPath<com.kaii.dentix.domain.type.SubscriptionStatus> status = createEnum("status", com.kaii.dentix.domain.type.SubscriptionStatus.class);

    public final DateTimePath<java.time.LocalDateTime> subscriptionEndDate = createDateTime("subscriptionEndDate", java.time.LocalDateTime.class);

    public final com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan subscriptionPlan;

    public final DateTimePath<java.time.LocalDateTime> subscriptionRenewalDate = createDateTime("subscriptionRenewalDate", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> subscriptionStartDate = createDateTime("subscriptionStartDate", java.time.LocalDateTime.class);

    public final NumberPath<Integer> successCount = createNumber("successCount", Integer.class);

    public final NumberPath<Double> usageRate = createNumber("usageRate", Double.class);

    public final DateTimePath<java.time.LocalDateTime> usageResetDate = createDateTime("usageResetDate", java.time.LocalDateTime.class);

    public QOrganizationSubscription(String variable) {
        this(OrganizationSubscription.class, forVariable(variable), INITS);
    }

    public QOrganizationSubscription(Path<? extends OrganizationSubscription> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrganizationSubscription(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrganizationSubscription(PathMetadata metadata, PathInits inits) {
        this(OrganizationSubscription.class, metadata, inits);
    }

    public QOrganizationSubscription(Class<? extends OrganizationSubscription> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new QOrganization(forProperty("organization"), inits.get("organization")) : null;
        this.subscriptionPlan = inits.isInitialized("subscriptionPlan") ? new com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan(forProperty("subscriptionPlan")) : null;
    }

}

