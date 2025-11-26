package com.kaii.dentix.domain.organization.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrganization is a Querydsl query type for Organization
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrganization extends EntityPathBase<Organization> {

    private static final long serialVersionUID = -1425560449L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrganization organization = new QOrganization("organization");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final BooleanPath active = createBoolean("active");

    public final ListPath<com.kaii.dentix.domain.admin.domain.Admin, com.kaii.dentix.domain.admin.domain.QAdmin> admins = this.<com.kaii.dentix.domain.admin.domain.Admin, com.kaii.dentix.domain.admin.domain.QAdmin>createList("admins", com.kaii.dentix.domain.admin.domain.Admin.class, com.kaii.dentix.domain.admin.domain.QAdmin.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final DateTimePath<java.time.LocalDateTime> deleted = createDateTime("deleted", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final StringPath organizationAddress = createString("organizationAddress");

    public final StringPath organizationEmail = createString("organizationEmail");

    public final NumberPath<Long> organizationId = createNumber("organizationId", Long.class);

    public final StringPath organizationName = createString("organizationName");

    public final StringPath organizationPhoneNumber = createString("organizationPhoneNumber");

    public final QOrganizationSubscription organizationSubscription;

    public final DateTimePath<java.time.LocalDateTime> subscriptionEndDate = createDateTime("subscriptionEndDate", java.time.LocalDateTime.class);

    public final ListPath<com.kaii.dentix.domain.subscription.domain.SubscriptionHistory, com.kaii.dentix.domain.subscription.domain.QSubscriptionHistory> subscriptionHistories = this.<com.kaii.dentix.domain.subscription.domain.SubscriptionHistory, com.kaii.dentix.domain.subscription.domain.QSubscriptionHistory>createList("subscriptionHistories", com.kaii.dentix.domain.subscription.domain.SubscriptionHistory.class, com.kaii.dentix.domain.subscription.domain.QSubscriptionHistory.class, PathInits.DIRECT2);

    public final com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan subscriptionPlan;

    public final DateTimePath<java.time.LocalDateTime> subscriptionStartDate = createDateTime("subscriptionStartDate", java.time.LocalDateTime.class);

    public QOrganization(String variable) {
        this(Organization.class, forVariable(variable), INITS);
    }

    public QOrganization(Path<? extends Organization> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrganization(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrganization(PathMetadata metadata, PathInits inits) {
        this(Organization.class, metadata, inits);
    }

    public QOrganization(Class<? extends Organization> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organizationSubscription = inits.isInitialized("organizationSubscription") ? new QOrganizationSubscription(forProperty("organizationSubscription"), inits.get("organizationSubscription")) : null;
        this.subscriptionPlan = inits.isInitialized("subscriptionPlan") ? new com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan(forProperty("subscriptionPlan")) : null;
    }

}

