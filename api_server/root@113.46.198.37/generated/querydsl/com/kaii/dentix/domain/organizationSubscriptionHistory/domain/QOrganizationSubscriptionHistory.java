package com.kaii.dentix.domain.organizationSubscriptionHistory.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrganizationSubscriptionHistory is a Querydsl query type for OrganizationSubscriptionHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrganizationSubscriptionHistory extends EntityPathBase<OrganizationSubscriptionHistory> {

    private static final long serialVersionUID = 697413025L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrganizationSubscriptionHistory organizationSubscriptionHistory = new QOrganizationSubscriptionHistory("organizationSubscriptionHistory");

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

    public final EnumPath<com.kaii.dentix.domain.type.SubscriptionStatus> status = createEnum("status", com.kaii.dentix.domain.type.SubscriptionStatus.class);

    public final com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan subscriptionPlan;

    public QOrganizationSubscriptionHistory(String variable) {
        this(OrganizationSubscriptionHistory.class, forVariable(variable), INITS);
    }

    public QOrganizationSubscriptionHistory(Path<? extends OrganizationSubscriptionHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrganizationSubscriptionHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrganizationSubscriptionHistory(PathMetadata metadata, PathInits inits) {
        this(OrganizationSubscriptionHistory.class, metadata, inits);
    }

    public QOrganizationSubscriptionHistory(Class<? extends OrganizationSubscriptionHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new com.kaii.dentix.domain.organization.domain.QOrganization(forProperty("organization"), inits.get("organization")) : null;
        this.subscriptionPlan = inits.isInitialized("subscriptionPlan") ? new com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan(forProperty("subscriptionPlan")) : null;
    }

}

