package com.kaii.dentix.domain.billing.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBilling is a Querydsl query type for Billing
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBilling extends EntityPathBase<Billing> {

    private static final long serialVersionUID = 377818817L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBilling billing = new QBilling("billing");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final NumberPath<Double> amount = createNumber("amount", Double.class);

    public final DateTimePath<java.time.LocalDateTime> billedAt = createDateTime("billedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.kaii.dentix.domain.type.BillingStatus> billingStatus = createEnum("billingStatus", com.kaii.dentix.domain.type.BillingStatus.class);

    public final EnumPath<com.kaii.dentix.domain.type.BillingType> billingType = createEnum("billingType", com.kaii.dentix.domain.type.BillingType.class);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final com.kaii.dentix.domain.organization.domain.QOrganization organization;

    public final DateTimePath<java.time.LocalDateTime> paidAt = createDateTime("paidAt", java.time.LocalDateTime.class);

    public final StringPath paymentRef = createString("paymentRef");

    public final DateTimePath<java.time.LocalDateTime> periodEnd = createDateTime("periodEnd", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> periodStart = createDateTime("periodStart", java.time.LocalDateTime.class);

    public final com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan subscriptionPlan;

    public QBilling(String variable) {
        this(Billing.class, forVariable(variable), INITS);
    }

    public QBilling(Path<? extends Billing> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBilling(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBilling(PathMetadata metadata, PathInits inits) {
        this(Billing.class, metadata, inits);
    }

    public QBilling(Class<? extends Billing> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new com.kaii.dentix.domain.organization.domain.QOrganization(forProperty("organization"), inits.get("organization")) : null;
        this.subscriptionPlan = inits.isInitialized("subscriptionPlan") ? new com.kaii.dentix.domain.subscription.domain.QSubscriptionPlan(forProperty("subscriptionPlan")) : null;
    }

}

